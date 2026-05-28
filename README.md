# Spiking Neural Engine
### A biologically-inspired, event-driven neural system with reinforcement learning

---

## Overview

This project implements a **spiking neural network (SNN)** inspired by biological brain systems, designed to process information through **time, space, and events** rather than static tensors.

Unlike traditional neural networks:

- ❌ No backpropagation  
- ❌ No fixed forward pass  
- ✅ Uses spikes, delays, and temporal dynamics  
- ✅ Learns via local plasticity and reward  

The system integrates:

- **STDP (Spike-Timing Dependent Plasticity)**
- **Eligibility traces for temporal credit assignment**
- **Reward-modulated learning**
- **Spatially organized cortical layers**
- **Adaptive stabilization mechanisms**

---

## High-Level Architecture
[ Retina (input) ]
↓
[ Cortical Layers (Spherical, 3D) ]
↓
[ OCR Classifier Neurons ]
↓
[ Supervisor (Reward / RL) ]

---

## Retina (Input Encoding)

The retina converts an image into spikes using a biologically-inspired mechanism:

- Detects **luminance changes (ON/OFF)**
- Applies **receptive fields (local averaging)**
- Simulates **microsaccades** (small eye movements)
- Encodes intensity as **spike frequency**

💡 Optimized using **integral images** for fast spatial sampling.

---

## Brain (Core SNN)

### Spatial Structure
- Neurons arranged on a **3D spherical surface**
- Distributed using **Fibonacci sphere**
- Connections are **local and distance-based**

### Synapses
- Transmit spikes with **propagation delays**
- Maintain spike queues (event-driven)
- Apply **plasticity rules**

### Execution Model
- Fully **event-driven**
- Only active neurons are processed
- Supports **parallel execution**

---

## Learning System

### 1. STDP (Timing-Based Learning)

Synaptic updates depend on spike timing:


Δw ∝ exp(-Δt / τ)

- Pre → Post → strengthen connection  
- Post → Pre → weaken connection  

---

### 2. Eligibility Traces

Each synapse maintains a decaying memory:


eligibility(t)

- stores recent timing relationships  
- decays over time  
- enables delayed learning  

---

### 3. Reward-Modulated Learning

Weights update via:


Δw = reward × eligibility × neuromodulator

This allows:

- delayed reinforcement  
- task-driven learning  
- biologically plausible credit assignment  

---

## OCR Classifier

The system performs character recognition using:

- 26 output neurons (A–Z)
- Spike accumulation per neuron
- Soft competition (winner-takes-most)

### Confidence


confidence = bestScore / sum(allScores)

---

## 🎓 Supervisor (Reinforcement Learning)

The supervisor evaluates the classifier output and generates a reward signal.

Reward depends on:

- ✅ correctness (expected vs predicted)  
- 📊 confidence (decision strength)  
- ⏱ timing (recency of activity)  


reward ∝ correctness × confidence × timing

The system then:

- reinforces correct neuron inputs  
- penalizes incorrect predictions  

---

## Adaptive Stabilization

A built-in controller continuously regulates the network:

### Local (per layer)
- firing threshold  
- membrane leak  
- STDP parameters  

### Inter-layer
- signal propagation strength  

### Global
- prevents:
  - inactivity (“dead network”)  
  - runaway excitation  

---

## Metrics & Observability

The system tracks:

- firing rate (active & global)
- sparsity
- synaptic weight distribution
- saturation ratios
- plasticity magnitude
- energy proxy
- activation stability

These metrics drive the stabilizer.

---

## Execution Model

The engine runs as a continuous loop:


while (running):
process active neurons
propagate spikes
update plasticity
apply reward signals
update metrics

✔ Sparse computation  
✔ Time-based simulation  
✔ No discrete epochs  

---

## Key Features

✅ Event-driven processing  
✅ Temporal learning  
✅ Local learning rules (no backpropagation)  
✅ Reinforcement learning integration  
✅ Spatial connectivity  
✅ Self-stabilizing dynamics  

---

## Trade-offs

- Non-deterministic (parallelism + timing)
- Requires parameter tuning
- Harder to debug than traditional models
- Sensitive to memory layout and performance optimizations

---

## Use Cases

- OCR (implemented example)
- Temporal pattern recognition
- Sequence learning
- Neuromorphic simulations
- Reinforcement learning environments

---

## Future Improvements

- Center-surround retina (edge detection)
- Motion detection (temporal filters)
- Advanced temporal RL
- SIMD / GPU optimization
- Hierarchical perception

---

## Philosophy

This project does **not** aim to replicate deep learning models.

Instead, it explores:

> how intelligent behavior can emerge from  
> local rules, time dynamics, and interaction

---

## Status

✅ Fully functional  
✅ OCR pipeline implemented  
✅ Adaptive and self-stabilizing  
✅ Ready for experimentation  

---

## Summary

This is not just a neural network.

> It is a dynamic spiking system combining  
> biology-inspired learning, temporal processing, and reinforcement.

---

## Contributing / Experiments

You can experiment with:

- new tasks (beyond OCR)
- reward shaping strategies
- stabilizer tuning
- temporal pattern learning

Pull requests and ideas are welcome 🚀
