package com.fitly.app.service;

import com.fitly.app.persistence.entity.FriendshipEntity;
import com.fitly.app.persistence.entity.FriendshipStatus;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.FriendshipJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.service.FriendService.FriendView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock private UserJpaRepository users;
    @Mock private FriendshipJpaRepository friendships;

    private FriendService service;
    private UserEntity ann;
    private UserEntity ben;

    @BeforeEach
    void setUp() {
        service = new FriendService(users, friendships);
        ann = Fixtures.user(1L, "ann@x.com");
        ben = Fixtures.user(2L, "ben@x.com");
    }

    @Test
    void youCannotAddYourself() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));

        assertThrows(IllegalArgumentException.class, () -> service.sendRequest("ann@x.com", "ann@x.com"));

        verify(friendships, never()).save(any());
    }

    @Test
    void unknownPersonIsNotFound() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(users.findByEmail("zed@x.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.sendRequest("ann@x.com", "zed@x.com"));
    }

    @Test
    void anExistingRequestInEitherDirectionBlocksANewOne() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(users.findByEmail("ben@x.com")).thenReturn(Optional.of(ben));
        when(friendships.existsByRequesterIdAndAddresseeIdOrRequesterIdAndAddresseeId(1L, 2L, 2L, 1L))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.sendRequest("ann@x.com", "ben@x.com"));

        verify(friendships, never()).save(any());
    }

    @Test
    void aNewRequestIsSavedAsPending() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(users.findByEmail("ben@x.com")).thenReturn(Optional.of(ben));

        service.sendRequest("ann@x.com", "  ben@x.com ");

        verify(friendships).save(any(FriendshipEntity.class));
    }

    @Test
    void pendingRequestsShowWhoSentThem() {
        when(users.findByEmail("ben@x.com")).thenReturn(Optional.of(ben));
        when(friendships.findByAddresseeIdAndStatus(2L, FriendshipStatus.PENDING))
                .thenReturn(List.of(new FriendshipEntity(ann, ben)));

        assertEquals("ann@x.com", service.pendingRequests("ben@x.com").get(0).fromEmail());
    }

    @Test
    void onlyTheAddresseeCanAnswer() {
        when(friendships.findById(9L)).thenReturn(Optional.of(new FriendshipEntity(ann, ben)));

        assertThrows(NotFoundException.class, () -> service.respond("ann@x.com", 9L, true));
        assertThrows(NotFoundException.class, () -> service.respond("carl@x.com", 9L, true));
    }

    @Test
    void acceptingMakesItAcceptedAndItCannotBeAnsweredTwice() {
        FriendshipEntity request = new FriendshipEntity(ann, ben);
        when(friendships.findById(9L)).thenReturn(Optional.of(request));

        service.respond("ben@x.com", 9L, true);

        assertEquals(FriendshipStatus.ACCEPTED, request.getStatus());
        assertThrows(NotFoundException.class, () -> service.respond("ben@x.com", 9L, false));
    }

    @Test
    void decliningMakesItDeclined() {
        FriendshipEntity request = new FriendshipEntity(ann, ben);
        when(friendships.findById(9L)).thenReturn(Optional.of(request));

        service.respond("ben@x.com", 9L, false);

        assertEquals(FriendshipStatus.DECLINED, request.getStatus());
    }

    @Test
    void friendsAreTheOtherPersonOnEitherSide() {
        UserEntity carl = Fixtures.user(3L, "carl@x.com");
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(friendships.findForUser(1L, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(new FriendshipEntity(ann, ben), new FriendshipEntity(carl, ann)));

        List<String> emails = service.friends("ann@x.com").stream().map(FriendView::email).toList();

        assertEquals(List.of("ben@x.com", "carl@x.com"), emails);
    }
}
