package com.cortex.base.dendritic_competition;

public class DendriticCompetitionStrategiesConfig {

	public WinnerTakeMostCompetitionConfig WINNER_TAKE_MOST_CONFIG;
	public NormalizedCompetitionConfig NORMALIZED_CONFIG;
	public ContinuousCompetitionConfig CONTINOUS_CONFIG;
	
	public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final DendriticCompetitionStrategiesConfig cfg;

        public Builder() {
            cfg = new DendriticCompetitionStrategiesConfig();

            // Default configs
            cfg.WINNER_TAKE_MOST_CONFIG = WinnerTakeMostCompetitionConfig.newBuilder().build();
            cfg.NORMALIZED_CONFIG       = NormalizedCompetitionConfig.newBuilder().build();
            cfg.CONTINOUS_CONFIG        = ContinuousCompetitionConfig.newBuilder().build();
        }

        public Builder withWinnerTakeMost(WinnerTakeMostCompetitionConfig c) {
            cfg.WINNER_TAKE_MOST_CONFIG = c;
            return this;
        }

        public Builder withNormalized(NormalizedCompetitionConfig c) {
            cfg.NORMALIZED_CONFIG = c;
            return this;
        }

        public Builder withContinuous(ContinuousCompetitionConfig c) {
            cfg.CONTINOUS_CONFIG = c;
            return this;
        }

        private void validate() {
            if (cfg.WINNER_TAKE_MOST_CONFIG == null)
                throw new IllegalArgumentException("WINNER_TAKE_MOST_CONFIG cannot be null");

            if (cfg.NORMALIZED_CONFIG == null)
                throw new IllegalArgumentException("NORMALIZED_CONFIG cannot be null");

            if (cfg.CONTINOUS_CONFIG == null)
                throw new IllegalArgumentException("CONTINOUS_CONFIG cannot be null");
        }

        public DendriticCompetitionStrategiesConfig build() {
            validate();
            return cfg;
        }
    }
}
