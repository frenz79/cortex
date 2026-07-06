package com.cortex.base.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.cortex.base.beans.NeuronBean;
import com.cortex.base.utils.IntList;
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
	
	public static final List<Neighbor> toNeighbors(IntList idxs, NeuronBean[] neurons, float px, float py, float pz) {
		ArrayList<Neighbor> out = new ArrayList<>(idxs.size());
		for (int i = 0; i < idxs.size(); i++) {
			int ni = idxs.get(i);
			float vx = neurons[ni].getPosition().x() - px;
			float vy = neurons[ni].getPosition().y() - py;
			float vz = neurons[ni].getPosition().z() - pz;
			float d = (float)Maths.sqrt(vx*vx + vy*vy + vz*vz);
			out.add(new Neighbor(neurons[ni], d, false));
		}
		return out;
	}
}