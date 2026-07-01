package com.evolve.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationResponse {

	private boolean shouldAnswer;
	private float confidence;
	private String reason;
	
}
