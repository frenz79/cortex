package com.cortex.base.layers;

import java.util.Objects;

import com.cortex.base.beans.NeuronBean;
import com.cortex.base.utils.Maths;

public record Neighbor(NeuronBean neuron, float distance, boolean near) { 

	public float getRealDistance() {
		return (float)Maths.sqrt(distance);
	}

	public NeuronBean neuron() {
		return neuron;
	}

	@Override
	public int hashCode() {
		return neuron.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Neighbor other = (Neighbor) obj;
		return Objects.equals(neuron, other.neuron);
	}	
}