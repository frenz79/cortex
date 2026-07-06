package com.cortex.base.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.Commons.SYNAPSE_SPEED;
import com.cortex.base.beans.NeuronBean;
import com.cortex.base.beans.SynapseBean;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.Functions;
import com.cortex.base.layers.LayerConnConfig;
import com.cortex.base.layers.Neighbor;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.soa.constants.BranchTypeCode;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.CorticalNeuronsConfig;

public final class SynapsesBuilder {

	final Logger logger = LogManager.getLogger(this.getClass());

	private final NeuronBean[] neurons;
	
	public SynapsesBuilder( NeuronBean[] neurons ) {
		this.neurons = neurons;
	}
		
	public int buildInternalSynapses( Abstract3DLayer layer ) {
		long startTime = System.nanoTime();
		NeuronBean[] neurons = layer.getNeurons();		
		int len = layer.getNeuronsLen();
		int start = layer.getNeuronsStart();
				
		int connectionsCount = 0;
		int localCount = (int)(layer.getConfig().MAX_CONNECTIONS * 0.8f);
		int farCount   = layer.getConfig().MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();

		Map<NeuronBean, List<Neighbor>> neighbors = new ConcurrentHashMap<>((int)(len*1.2f));
		Arrays.stream(neurons,start,start+len).parallel().forEach( n -> {
			try {
				List<Neighbor> local = new ArrayList<>(
					Functions.findNearestNeurons(neurons, start, len, n, localCount, layer.getConfig().CONNECTION_FILTER)
				);
	
				List<Neighbor> far = Functions.findRandomNeurons(neurons, start, len, n, farCount);
				List<Neighbor> all = new ArrayList<>(local.size() + far.size());
				all.addAll(local);
				all.addAll(far);
				neighbors.put(n, all);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		});

		for (int round = 0; round < layer.getConfig().MAX_CONNECTIONS; round++) {
			for (int i=0; i<len; i++) {
				NeuronBean src = neurons[i+start];
				List<Neighbor> neigh = neighbors.get(src);
				if (neigh.isEmpty()) continue;

				Neighbor target = neigh.remove(neigh.size() - 1);
				NeuronBean dst = target.neuron();

				if (src == dst) continue;

				Point3f ps = src.getPosition();
				Point3f pd = dst.getPosition();

				float ds = ps.x()*ps.x() + ps.y()*ps.y() + ps.z()*ps.z();
				float dd = pd.x()*pd.x() + pd.y()*pd.y() + pd.z()*pd.z();

				if (ds >= dd) continue;
						
				var s = new SynapseBean(
					src.id, 
					dst.id, 
					target.getRealDistance()
				);
				
				int branchType = target.near()?BranchTypeCode.NEAR:BranchTypeCode.FAR;
				src.addOutgoingSynapse(s, branchType);
				src.addIncomingSynapse(s, branchType);
				connectionsCount++;
			}
		}

		// Check all neurons have at least one input / output
		for (int i=0; i<len; i++) {
			NeuronBean src = neurons[i+start];		
			if ( !src.hasIncoming() || !src.hasOutgoing() ) {
				List<Neighbor> far = Functions.findRandomNeurons(neurons, start, len, src, farCount);
				while(!far.isEmpty()) {

					Neighbor target = far.remove(far.size() - 1);
					NeuronBean dst = target.neuron();

					if (src == dst) continue;

					Point3f ps = src.getPosition();
					Point3f pd = dst.getPosition();

					float ds = ps.x()*ps.x() + ps.y()*ps.y() + ps.z()*ps.z();
					float dd = pd.x()*pd.x() + pd.y()*pd.y() + pd.z()*pd.z();

					if (ds >= dd) continue;
					
					var s = new SynapseBean(
						src.id, 
						dst.id, 
						target.getRealDistance()
					);
					
					if (!src.isAlreadyConnectedTo(dst)) {
						src.addOutgoingSynapse(s, BranchTypeCode.FAR);
						src.addIncomingSynapse(s, BranchTypeCode.FAR);
						connectionsCount++;
						break;
					}
				}
			}
		}

		long endTime = System.nanoTime();
		logger.info(
			"L{} generated {} synapses in {}",
			layer.getLayerId(),
			connectionsCount,
			TimeUnit.NANOSECONDS.toMicros(endTime - startTime) + " micros"
		);
		return connectionsCount;
	}	

	// Generate Synapses between layers	
	public void buildLayersSynapses( List<LayerConnConfig> configs ) {	
		configs.parallelStream().forEach(
			e -> {
			Abstract3DLayer srcLayer = e.SOURCE_LAYER;
			Abstract3DLayer dstLayer = e.TARGET_LAYER;
			int connections = 0;
			
			connections += link(
				srcLayer,
				dstLayer, 
				e.MIN_CONNECTIONS, 
				e.MAX_CONNECTIONS,
				e.MAX_DISTANCE, 
				e.BASE_SPEED,
				e.NEURON_FILTER_PREDICATE,
				e.SYNAPSE_PLASTICITY_CONFIG
			);
			
			logger.info("L{} -> L{} : created {} synapses",
				srcLayer.getLayerId(),	
				dstLayer.getLayerId(),
				connections
			);
		});	
		logger.info("buildLayersSynapses..finished!");
	}

	private int link(
			Abstract3DLayer sourceLayer,
			Abstract3DLayer targetLayer,
			int minConn,
			int maxConn,
			float maxDistance,
			long baseSpeed,
			Predicate<NeuronBean> filter,
			SynapsePlasticityConfig plasticityCfg) {

		NeuronBean[] srcs = sourceLayer.getNeurons();
		NeuronBean[] dsts = targetLayer.getNeurons();
		
		CorticalNeuronsConfig neuronsConfig = targetLayer.getConfig().CORTICAL_NEURONS_CONFIG;

		int N = srcs.length;
		AtomicInteger connectionsCount = new AtomicInteger(0);

		// Precalcolo vicini per ogni sorgente
		Map<NeuronBean, List<Neighbor>> neighbors = new ConcurrentHashMap<>(N*2);
		Arrays.stream(srcs, sourceLayer.getNeuronsStart(), sourceLayer.getNeuronsStart()+sourceLayer.getNeuronsLen()).parallel().forEach( src -> {
			neighbors.put(src, new ArrayList<>(
				Functions.findNearestNeurons(dsts, targetLayer.getNeuronsStart(), targetLayer.getNeuronsLen(), src, maxConn, filter)
			));
		});

		int bt = (sourceLayer.getLayerId()<targetLayer.getLayerId())
				?BranchTypeCode.FEEDFORWARD
				:BranchTypeCode.FEEDBACK;
		
		// Round-robin
		for (int round = 0; round < maxConn; round++) {
			//for (AbstractNeuron src : srcs) {
			Arrays.stream(srcs, sourceLayer.getNeuronsStart(), sourceLayer.getNeuronsStart()+sourceLayer.getNeuronsLen()).parallel().forEach( src -> {
				List<Neighbor> neigh = neighbors.get(src);
				if (!neigh.isEmpty()) {

					int attempts = neigh.size();
					for (int i = 0; i < attempts; i++) {

						Neighbor target = neigh.remove(0);
						NeuronBean dst = target.neuron();

						if (src == dst) continue;

						Point3f ps = src.getPosition();
						Point3f pd = dst.getPosition();
						// Don't mind Z pos for inter-layers connections
						float ds = ps.x()*ps.x() + ps.y()*ps.y();// + ps.z()*ps.z();
						float dd = pd.x()*pd.x() + pd.y()*pd.y();// + pd.z()*pd.z();

						if (ds >= dd) continue;
						if ( dst.getIncomingSynapses()>=neuronsConfig.MAX_FAN_IN ) {
							continue;
						}
						if ( src.getOutgoingSynapses()>=neuronsConfig.MAX_FAN_OUT ) {
							continue;
						}
						
						var s = new SynapseBean(
								src.id, 
								dst.id, 
								target.getRealDistance()
							);
							
						if (!src.isAlreadyConnectedTo(dst)) {
							src.addOutgoingSynapse(s, bt);
							src.addIncomingSynapse(s, bt);
							connectionsCount.incrementAndGet();
							break;
						}							
					}
				}
			});
		}
		return connectionsCount.get();
	}
}
