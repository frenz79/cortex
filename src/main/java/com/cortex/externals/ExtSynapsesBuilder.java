package com.cortex.externals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import com.cortex.base.Commons.SYNAPSE_SPEED;
import com.cortex.base.beans.NeuronBean;
import com.cortex.base.beans.SynapseBean;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.Functions;
import com.cortex.base.layers.Neighbor;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.constants.BranchTypeCode;
import com.cortex.base.soa.constants.DirectionCode;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

public class ExtSynapsesBuilder {

	public int link( AbstractExtModuleLogic ext, Abstract3DLayer targetLayer ) {
		Point3f pluginSite = ext.getPluginSite();
		//	ext.getExternalConnConfig().CONNECTIONS,
		//	ext.getExternalConnConfig().MAX_DISTANCE,
		//	ext.getExternalConnConfig().NEURON_FILTER_PREDICATE,
		//	ext.getExternalConnConfig().SYNAPSE_PLASTICITY_CONFIG
		
		Predicate<NeuronBean> filter = null;
		int extNeuronsCount = ext.getNeuronsCount();
		int maxConn = 250;
		float maxDistance = 250.0f;
		
		// Sphere projection
		float u = (pluginSite.x() + 0.5f); // 0..1
		float v = (pluginSite.y() + 0.5f); // 0..1

		float theta = (float)(2 * Maths.PI * u);     // longitude
		float phi   = (float)(Maths.PI * (v - 0.5)); // latitude
		float cosPhi = (float)Maths.cos(phi);

		float x = (float)(cosPhi * Maths.cos(theta) * targetLayer.getConfig().DIMENSION);
		float y = (float)(cosPhi * Maths.sin(theta) * targetLayer.getConfig().DIMENSION);
		float z = (float)(Maths.sin(phi) * targetLayer.getConfig().DIMENSION);

		Random rnd = ThreadLocalRandom.current();
		IntList neighborsIdx = Functions.findKNearestApprox(
				x,y,z, maxConn, maxDistance, maxDistance,
				targetLayer.getNeurons(), targetLayer.getSpatialHash());

		List<Neighbor> conns = Neighbor.toNeighbors(neighborsIdx, targetLayer.getNeurons(), x,y,z);
		conns.removeIf(n -> !filter.test(n.neuron()));
		conns.removeIf(n -> n.getRealDistance() > maxDistance);
		
		List<NeuronBean> extNeurons = new ArrayList<>();	// TODO
 		Set<SynapseBean> extSynapses = new HashSet<>();
		
 		int connectionsCount = 0;
 		
		for (int extId=0; extId<extNeuronsCount; extId++) {
			NeuronBean extNeuron = extNeurons.get(extId);
			
			List<Integer> rndIdx = new ArrayList<>(maxConn);
			for (int j=0; j<maxConn; j++ ) {
				rndIdx.add( rnd.nextInt(0, conns.size()) );
			}
			
			for ( int i : rndIdx ) {
				var intNeuron = conns.get(i);
	
				var s = new SynapseBean(
					i, 
					intNeuron.neuron().id, 
					intNeuron.getRealDistance()
				);
				
				if (!extSynapses.contains(s)) {
					if (ext.getDirection()==DirectionCode.OUTGOING) {
						extNeuron.addOutgoingSynapse(s, BranchTypeCode.EXTERNAL);
						intNeuron.neuron().addIncomingSynapse(s, BranchTypeCode.EXTERNAL);
					} else {
						extNeuron.addIncomingSynapse(s, BranchTypeCode.EXTERNAL);
						intNeuron.neuron().addOutgoingSynapse(s, BranchTypeCode.EXTERNAL);
					}
					connectionsCount++;
					break;
				}
			}
		}
		
		ExtSynapseTopologySoA extSynapseTopologySoA = new ExtSynapseTopologySoA( extNeuronsCount );
		ExtSynapseSoA extSynapseSoA = new ExtSynapseSoA( extSynapses.size() );
		// Build / Update SOA
		int synIdx = 0;
		for ( SynapseBean s : extSynapses ) {
			extSynapseSoA.sourceNeuronId[synIdx] = s.sourceNeuronId;
			extSynapseSoA.targetNeuronId[synIdx] = s.targetNeuronId;
			extSynapseSoA.length[synIdx] = s.length;
			extSynapseSoA.baseSpeed[synIdx] = SYNAPSE_SPEED.FAST.getBaseSpeed();
			// TODO	
			synIdx++;
		}
		
		ExtNeuronSoA extNeuronSoA = new ExtNeuronSoA(extNeuronsCount);
		for ( int idx=0; idx<extNeurons.size(); idx++ ) {
			// TODO			
		}
		return connectionsCount;
	}
	
	public NeuronSoA buildNeuronSoA(NeuronBean[] neurons) {
		NeuronSoA ret = new NeuronSoA(neurons.length);
		for ( int i=0; i<neurons.length; i++ ) {
			Point3f p = neurons[i].position;
			ret.posX[i] = p.x();
			ret.posY[i] = p.y();
			ret.posZ[i] = p.z();
			ret.setLayerId(i, neurons[i].getLayerId());
			if (neurons[i].isInhibitor()) {
				ret.setInhibitory(i);
			}			
		}		
		return ret;
	}
}
