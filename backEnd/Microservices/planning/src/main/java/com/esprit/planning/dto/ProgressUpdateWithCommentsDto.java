package com.esprit.planning.dto;

import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Progress update along with its associated comments")
public class ProgressUpdateWithCommentsDto {

    @Schema(description = "The progress update")
    private ProgressUpdate progressUpdate;

    @Schema(description = "Comments attached to this progress update")
    private List<ProgressComment> comments;
}

