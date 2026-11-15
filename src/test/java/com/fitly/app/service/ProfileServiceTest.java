package com.fitly.app.service;

import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.service.ProfileService.ProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserJpaRepository users;

    private ProfileService service;
    private UserEntity ann;

    @BeforeEach
    void setUp() {
        service = new ProfileService(users);
        ann = Fixtures.user(1L, "ann@x.com");
        lenient().when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
    }

    @Test
    void updateTrimsAndSaves() {
        ProfileView view = service.update("ann@x.com", "  Ann  ", " Runner ");

        assertEquals("Ann", view.displayName());
        assertEquals("Runner", ann.getBio());
    }

    @Test
    void blankTextClearsTheField() {
        ann.setDisplayName("Old");

        ProfileView view = service.update("ann@x.com", "   ", null);

        assertNull(view.displayName());
        assertNull(view.bio());
    }

    @Test
    void nameLongerThan50IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.update("ann@x.com", "x".repeat(51), null));
    }

    @Test
    void bioLongerThan500IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.update("ann@x.com", null, "x".repeat(501)));
    }

    @Test
    void getShowsEmailNameAndBio() {
        ann.setDisplayName("Ann");

        ProfileView view = service.get("ann@x.com");

        assertEquals("ann@x.com", view.email());
        assertEquals("Ann", view.displayName());
    }
}
