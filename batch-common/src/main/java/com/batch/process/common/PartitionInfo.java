package com.batch.process.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
@AllArgsConstructor
@Builder
@JsonSerialize
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartitionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private int partitionCount;

    @Getter
    private int totalCount;

    @Getter
    private int batchCount;

    @Builder.Default
    private List<Integer> offsets = new ArrayList<>();

    public boolean addOffset(int offset) {
        return offsets.add(offset);
    }

    public List<Integer> getOffsets() {
        return Collections.unmodifiableList(offsets);
    }

    private String configurationName;

}
