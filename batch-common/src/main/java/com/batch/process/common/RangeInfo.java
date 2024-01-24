package com.batch.process.common;

import java.io.Serializable;

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
@Getter
@JsonDeserialize
@JsonSerialize
public class RangeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private long minId;
    private long maxId;
}
