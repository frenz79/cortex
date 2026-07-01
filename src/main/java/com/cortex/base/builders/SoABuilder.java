package com.cortex.base.builders;

import java.util.List;

import com.cortex.base.beans.BranchBean;
import com.cortex.base.beans.NeuronBean;
import com.cortex.base.beans.SynapseBean;
import com.cortex.base.soa.DendriticTreeSoA;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.NeuronTopologySoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.soa.constants.BranchTypeCode;
import com.cortex.base.soa.constants.DirectionCode;
import com.cortex.base.utils.Point3f;

public class SoABuilder {

	public SoABuilder() {
		
	}
	
	public static record SoABuilderResult(
		NeuronSoA neuronSoA,
		SynapseBranchSoA synapseBranchSoA,
		SynapseSoA synapseSoA,
		SynapseTopologySoA synapseTopologySoA,
		NeuronTopologySoA neuronTopologySoA,
		DendriticTreeSoA dendriticTreeSoA
	) {/**/}
	
	public SoABuilderResult buildAll( NeuronBean[] neurons ) {
		NeuronSoA neuronSoA = buildNeuronSoA( neurons);
		SynapseBranchSoA synapseBranchSoA = buildSynapseBranchSoA( neurons );
		SynapseSoA synapseSoA = buildSynapseSoA( neurons, synapseBranchSoA);
		SynapseTopologySoA synapseTopologySoA = buildSynapseTopologySoA( synapseBranchSoA, synapseSoA );
		NeuronTopologySoA neuronTopologySoA = buildNeuronTopologySoA( neurons, synapseBranchSoA );
		DendriticTreeSoA dendriticTreeSoA = buildDendriticTreeSoA( neurons );
		return new SoABuilderResult(
			neuronSoA,
			synapseBranchSoA,
			synapseSoA,
			synapseTopologySoA,
			neuronTopologySoA,
			dendriticTreeSoA
		);
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
	
	public SynapseBranchSoA buildSynapseBranchSoA(NeuronBean[] neurons) {
	    // Numero reale di branch creati in parallelo
	    int totBranches = BranchBean.idGen.get();

	    SynapseBranchSoA ret = new SynapseBranchSoA(totBranches);

	    // Mappa ID → BranchBean
	    BranchBean[] byId = new BranchBean[totBranches];

	    // Raccogliamo tutti i branch
	    for (NeuronBean n : neurons) {

	        for (List<BranchBean> list : n.outgoingBranches.values()) {
	            for (BranchBean b : list) {
	                byId[b.id] = b;
	            }
	        }
	        for (List<BranchBean> list : n.incomingBranches.values()) {
	            for (BranchBean b : list) {
	                byId[b.id] = b;
	            }
	        }
	    }

	    // Costruiamo il SoA in ordine di ID
	    for (int id = 0; id < totBranches; id++) {
	        BranchBean b = byId[id];
	        // ID del branch = indice nel SoA
	        ret.branchId[id] = id;
	        // Tipo del branch
	        ret.setBranchType(id, b.type);
	        // Direzione del branch
	        // (puoi aggiungere un flag in BranchBean per incoming/outgoing)
	        ret.setDirection(id, b.incoming ? DirectionCode.INCOMING : DirectionCode.OUTGOING);

	        // Numero di sinapsi nel branch
	        ret.synapseCount[id] = (byte)b.size();

	        // synapseStart verrà riempito dal SynapseSoABuilder
	    }

	    return ret;
	}
	
/*
	public SynapseBranchSoA buildSynapseBranchSoA(NeuronBean[] neurons) {
		int totBranches = countBranches(neurons);		
		
		SynapseBranchSoA ret = new SynapseBranchSoA(totBranches);

		int idx = 0;
		for (NeuronBean n : neurons) {
			for (List<BranchBean> branches : n.outgoingBranches.values()) {
				for ( BranchBean b : branches ) {
					ret.branchId[idx] = b.id;
					ret.setBranchType(idx, b.type);
					ret.setDirection(idx, DirectionCode.OUTGOING);
					ret.synapseCount[idx] = (byte)b.size();
					// synapseStart is filled in synapses builder
					idx++;
				}
			}
			for (List<BranchBean> branches : n.incomingBranches.values()) {
				for ( BranchBean b : branches ) {
					ret.branchId[idx] = b.id;
					ret.setBranchType(idx, b.type);
					ret.setDirection(idx, DirectionCode.INCOMING);
					ret.synapseCount[idx] = (byte)b.size();
					// synapseStart is filled in synapses builder
					idx++;
				}
			}
		}
		return ret;
	}
*/
	public SynapseSoA buildSynapseSoA(NeuronBean[] neurons, SynapseBranchSoA branchSoA) {
	    int totalSynapses = countSynapses(neurons);
	    SynapseSoA ret = new SynapseSoA(totalSynapses);

	    int synIdx = 0;
	    int branchIdx = 0;

	    // OUTGOING + INCOMING
	    for (NeuronBean n : neurons) {
	        // OUTGOING
	        for (List<BranchBean> list : n.outgoingBranches.values()) {
	            for (BranchBean b : list) {
	                // Punto di inizio delle sinapsi del branch
	                branchSoA.synapseStart[branchIdx] = synIdx;

	                for (SynapseBean s : b.getSynapses()) {
	                    ret.sourceNeuronId[synIdx] = s.sourceNeuronId;
	                    ret.targetNeuronId[synIdx] = s.targetNeuronId;
	                    ret.length[synIdx] = s.length;
	                    // branchId = indice del branch
	                    ret.branchIndex[synIdx] = branchIdx;

	                    synIdx++;
	                }
	                branchIdx++;
	            }
	        }

	        // INCOMING
	        for (List<BranchBean> list : n.incomingBranches.values()) {
	            for (BranchBean b : list) {
	                branchSoA.synapseStart[branchIdx] = synIdx;

	                for (SynapseBean s : b.getSynapses()) {
	                    ret.sourceNeuronId[synIdx] = s.sourceNeuronId;
	                    ret.targetNeuronId[synIdx] = s.targetNeuronId;
	                    ret.length[synIdx] = s.length;

	                    ret.branchIndex[synIdx] = branchIdx;

	                    synIdx++;
	                }
	                branchIdx++;
	            }
	        }
	    }
	    return ret;
	}

	public SynapseTopologySoA buildSynapseTopologySoA(SynapseBranchSoA branchSoA, SynapseSoA synapseSoA) {

	    int totalBranches = branchSoA.synapseStart.length;
	    int totalSynapses = synapseSoA.sourceNeuronId.length;

	    SynapseTopologySoA topo = new SynapseTopologySoA(totalBranches, totalSynapses);

	    // Copia diretta dei dati dai branch
	    System.arraycopy(branchSoA.synapseStart, 0, topo.synapseStart, 0, totalBranches);
	    System.arraycopy(branchSoA.synapseCount, 0, topo.synapseCount, 0, totalBranches);

	    // Generazione della lista contigua degli indici
	    int idx = 0;
	    for (int b = 0; b < totalBranches; b++) {

	        int start = branchSoA.synapseStart[b];
	        int count = branchSoA.synapseCount[b];

	        for (int i = 0; i < count; i++) {
	            topo.synapseIndex[idx++] = start + i;
	        }
	    }
	    return topo;
	}
	
	public NeuronTopologySoA buildNeuronTopologySoA(
	        NeuronBean[] neurons,
	        SynapseBranchSoA branchSoA
	) {
	    int N = neurons.length;
	    int totalBranches = branchSoA.synapseStart.length;

	    int totalGroups = 0;
	    for (NeuronBean nb : neurons) {
	        for (List<BranchBean> list : nb.incomingBranches.values()) {
	            totalGroups += list.size();
	        }
	    }

	    NeuronTopologySoA topo = new NeuronTopologySoA(N, totalBranches, totalGroups);

	    int incomingWrite = 0;
	    int outgoingWrite = 0;
	    int groupWrite = 0;

	    for (int n = 0; n < N; n++) {

	        NeuronBean nb = neurons[n];

	        // --- INCOMING ---
	        int incomingCount = nb.incomingBranches.values()
	                .stream().mapToInt(List::size).sum();

	        topo.incomingBranchStart[n] = incomingWrite;
	        topo.incomingBranchCount[n] = incomingCount;

	        for (List<BranchBean> list : nb.incomingBranches.values()) {
	            for (BranchBean b : list) {
	                topo.incomingBranches[incomingWrite++] = b.id;
	            }
	        }

	        // --- OUTGOING ---
	        int outgoingCount = nb.outgoingBranches.values()
	                .stream().mapToInt(List::size).sum();

	        topo.outgoingBranchStart[n] = outgoingWrite;
	        topo.outgoingBranchCount[n] = outgoingCount;

	        for (List<BranchBean> list : nb.outgoingBranches.values()) {
	            for (BranchBean b : list) {
	                topo.outgoingBranches[outgoingWrite++] = b.id;
	            }
	        }

	        // --- GROUPS ---
	        for (int t = 0; t < BranchTypeCode.size(); t++) {

	            int groupStartIndex = n * BranchTypeCode.size() + t;
	            topo.branchGroupStart[groupStartIndex] = groupWrite;

	            int countT = 0;
	            for (List<BranchBean> list : nb.incomingBranches.values()) {
	                for (BranchBean b : list) {
	                    if (b.type == t) countT++;
	                }
	            }

	            topo.branchGroupCount[groupStartIndex] = countT;

	            for (List<BranchBean> list : nb.incomingBranches.values()) {
	                for (BranchBean b : list) {
	                    if (b.type == t) {
	                        topo.branchGroupIndices[groupWrite++] = b.id;
	                    }
	                }
	            }
	        }
	    }

	    return topo;
	}

	
	public DendriticTreeSoA buildDendriticTreeSoA(NeuronBean[] neurons) {
	    int N = neurons.length;
	    DendriticTreeSoA tree = new DendriticTreeSoA(N);

	    int branchIdx = 0; // indice globale dei branch

	    for (int n = 0; n < N; n++) {
	        NeuronBean nb = neurons[n];

	        // Conta branch incoming + outgoing
	        int count = 0;

	        for (List<BranchBean> list : nb.outgoingBranches.values()) {
	            count += list.size();
	        }
	        for (List<BranchBean> list : nb.incomingBranches.values()) {
	            count += list.size();
	        }

	        tree.firstBranchIndex[n] = branchIdx;
	        tree.branchCount[n] = (byte) count;

	        branchIdx += count;
	    }

	    return tree;
	}

	private int countSynapses(NeuronBean[] neurons) {
	    int count = 0;

	    for (NeuronBean n : neurons) {
	        for (List<BranchBean> list : n.outgoingBranches.values()) {
	            for (BranchBean b : list) {
	                count += b.size();
	            }
	        }
	        for (List<BranchBean> list : n.incomingBranches.values()) {
	            for (BranchBean b : list) {
	                count += b.size();
	            }
	        }
	    }
	    return count;
	}
	
	/*
	 
	 private int countSynapses(NeuronBean[] neurons) {
	    Set<SynapseBean> synapses = new HashSet<>();
	    for (NeuronBean n : neurons) {
	        for (List<BranchBean> list : n.outgoingBranches.values()) {
	            for (BranchBean b : list) {
	            	synapses.addAll( b.getSynapses() );
	            }
	        }
	        
	        for (List<BranchBean> list : n.incomingBranches.values()) {
	            for (BranchBean b : list) {
	            	synapses.addAll( b.getSynapses() );
	            }
	        }
	    }
	    return synapses.size();
	}
	 
	
	 */
	
	private int countBranches(NeuronBean[] neurons) {
	    int count = 0;
		for (NeuronBean n : neurons) {
			for (List<BranchBean> list : n.outgoingBranches.values()) {
				count += list.size();
			}
			for (List<BranchBean> list : n.incomingBranches.values()) {
				count += list.size();
			}
		}
		return count;
	}
	
}
