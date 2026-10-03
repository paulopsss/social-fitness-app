package com.fitly.app;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

public record Workout(long id,
                      @JsonIgnore String owner,
                      String type,
                      int minutes,
                      LocalDate date,
                      String notes) {
}