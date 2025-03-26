package com.userPresence1.userPresence1;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@Entity
public class Note {

    @Id
    private UUID id;

    private String title;
    private String content;

}
