package com.cortex.brain;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CorticalNeuronsConfig {

	public int MAX_FAN_IN = 250;
	public int MAX_FAN_OUT = 250;
		
	public float FIRING_THRESHOLD = 0.12f;
	public long  REFRACTORY_PERIOD_NANOS = TimeUnit.MILLISECONDS.toNanos(5);
	public long  RATE_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
	public float RATE_DECAY_PER_WINDOW = 0.95f; // per RATE_WINDOW

	public float BRANCH_TARGET_ACTIVITY = 0.2f;
	public float BRANCH_GAIN_MIN = 0.1f;
	public float BRANCH_GAIN_MAX = 3.0f;
	public long  BRANCH_GAIN_TAU_NANOS = 50_000_000L; // 50 ms
	
	public int MAX_SYNAPSES_PER_BRANCH = 32;
	
//	public DendriticCompetitionStrategiesConfig DENDIRITIC_COMPETITION_STRATEGIES_CONFIG;
	
//	public final Map<BranchType,IDendriticCompetitionStrategy> DENDIRITIC_COMPETITION_STRATEGIES = new EnumMap<>(BranchType.class);

	public long INHIBITION_TAU_NANOS = 50_000_000L; // 50 ms
	public float INHIBITION_DECAY_PER_WINDOW = 0.95f;

	private CorticalNeuronsConfig() {
		/*
		DENDIRITIC_COMPETITION_STRATEGIES_CONFIG = DendriticCompetitionStrategiesConfig.newBuilder().build();
		
		DENDIRITIC_COMPETITION_STRATEGIES.put(
		    BranchType.NEAR,
		    new WinnerTakeMostCompetition(DENDIRITIC_COMPETITION_STRATEGIES_CONFIG.WINNER_TAKE_MOST_CONFIG)
		);
		DENDIRITIC_COMPETITION_STRATEGIES.put(
		    BranchType.FAR,
		    new ContinuousCompetition(DENDIRITIC_COMPETITION_STRATEGIES_CONFIG.CONTINOUS_CONFIG)
		);
		DENDIRITIC_COMPETITION_STRATEGIES.put(
		    BranchType.LAYER_FEEDFORWARD,
		    new NormalizedCompetition(DENDIRITIC_COMPETITION_STRATEGIES_CONFIG.NORMALIZED_CONFIG)
		);
		*/
		// No competition for EXTERNAL
	}
	
	public static Builder newBuilder() {
		return new Builder();
	}	

	public static class Builder {
		private final CorticalNeuronsConfig cfg;

		public Builder() {
			this.cfg = new CorticalNeuronsConfig();
		}

		public Builder withMaxFanInFanOut(int MAX_FAN_IN, int MAX_FAN_OUT) {
			cfg.MAX_FAN_IN = MAX_FAN_IN;
			cfg.MAX_FAN_OUT = MAX_FAN_OUT;
			return this;
		}

		public Builder withRefractoryPeriodNanos(long REFRACTORY_PERIOD_NANOS) {
			cfg.REFRACTORY_PERIOD_NANOS = REFRACTORY_PERIOD_NANOS;
			return this;
		}

		public Builder withFiringThreshold(float FIRING_THRESHOLD) {
			cfg.FIRING_THRESHOLD = FIRING_THRESHOLD;
			return this;
		}
/*
		public Builder withDendriticCompetitionStrategiesConfig(DendriticCompetitionStrategiesConfig DENDIRITIC_COMPETITION_STRATEGIES) {
			cfg.DENDIRITIC_COMPETITION_STRATEGIES_CONFIG = DENDIRITIC_COMPETITION_STRATEGIES;
			return this;
		}
*/		
		public Builder withInhibitionDecay( long INHIBITION_TAU_NANOS, float INHIBITION_DECAY_PER_WINDOW ) {		
			cfg.INHIBITION_TAU_NANOS = INHIBITION_TAU_NANOS;
			cfg.INHIBITION_DECAY_PER_WINDOW = INHIBITION_DECAY_PER_WINDOW;
			return this;
		}

		public Builder withRatePerSecond( long RATE_WINDOW_SECOND, float RATE_DECAY_PER_WINDOW ) {		
			cfg.RATE_WINDOW_NANOS = (long)(TimeUnit.SECONDS.toNanos(1)*RATE_WINDOW_SECOND);;
			cfg.RATE_DECAY_PER_WINDOW = RATE_DECAY_PER_WINDOW;
			return this;
		}
		
		private void validate() {
			if (cfg.RATE_WINDOW_NANOS <= 0)
				throw new IllegalArgumentException("RATE_WINDOW must be > 0");

			if (cfg.RATE_DECAY_PER_WINDOW <= 0 || cfg.RATE_DECAY_PER_WINDOW > 1)
				throw new IllegalArgumentException("RATE_DECAY_PER_WINDOW must be in (0,1]");
		}

		public CorticalNeuronsConfig build() {
			validate();
			/*
			 // Rebuild strategies based on the final config
	        cfg.DENDIRITIC_COMPETITION_STRATEGIES.clear();
	        cfg.DENDIRITIC_COMPETITION_STRATEGIES.put(
	            BranchType.NEAR,
	            new WinnerTakeMostCompetition(cfg.DENDIRITIC_COMPETITION_STRATEGIES_CONFIG.WINNER_TAKE_MOST_CONFIG)
	        );
	        cfg.DENDIRITIC_COMPETITION_STRATEGIES.put(
	            BranchType.FAR,
	            new ContinuousCompetition(cfg.DENDIRITIC_COMPETITION_STRATEGIES_CONFIG.CONTINOUS_CONFIG)
	        );
	        cfg.DENDIRITIC_COMPETITION_STRATEGIES.put(
	            BranchType.LAYER_FEEDFORWARD,
	            new NormalizedCompetition(cfg.DENDIRITIC_COMPETITION_STRATEGIES_CONFIG.NORMALIZED_CONFIG)
	        );
	        */
			return cfg;
		}
	}
}
