package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;
import com.cortex.base.utils.IntList;

/**
 * Spatial hash optimized for SoA neuron storage.
 * Stores only neuron indices, no objects.
 * Each layer has its own spatial grid.
 */
@SerializableClass
public final class SpatialHashSoA {

    // Grid parameters per layer
	@SerializableAttribute
    public final float[] cellSize;
	@SerializableAttribute
    public final int[] cellsX;
	@SerializableAttribute
    public final int[] cellsY;
	@SerializableAttribute
    public final int[] cellsZ;

    // For each layer, each cell stores a list of neuron indices
    // Flattened as: cellIndex = (z * cellsY + y) * cellsX + x
	@SerializableAttribute
    public final IntList[][] cellNeurons;

    // Reference to neuron positions
	@SerializableAttribute
    private final NeuronSoA neuronState;

    public SpatialHashSoA(int totalLayers, NeuronSoA neuronState) {
        this.neuronState = neuronState;

        this.cellSize = new float[totalLayers];
        this.cellsX = new int[totalLayers];
        this.cellsY = new int[totalLayers];
        this.cellsZ = new int[totalLayers];

        // One IntList per cell across all layers
        this.cellNeurons = new IntList[totalLayers][];
    }

    /**
     * Initializes the spatial grid for a specific layer.
     */
    public void initLayer(int layer, float cellSize, int cx, int cy, int cz) {
        this.cellSize[layer] = cellSize;
        this.cellsX[layer] = cx;
        this.cellsY[layer] = cy;
        this.cellsZ[layer] = cz;

        int totalCells = cx * cy * cz;
        IntList[] arr = new IntList[totalCells];
        for (int i = 0; i < totalCells; i++)
            arr[i] = new IntList();

        this.cellNeurons[layer] = arr;
    }

    /**
     * Inserts a neuron into the correct spatial cell.
     */
    public void insertNeuron(int neuronId) {
        int layer = neuronState.getLayerId(neuronId);

        float x = neuronState.posX[neuronId];
        float y = neuronState.posY[neuronId];
        float z = neuronState.posZ[neuronId];

        int cx = (int)(x / cellSize[layer]);
        int cy = (int)(y / cellSize[layer]);
        int cz = (int)(z / cellSize[layer]);

        int idx = cellIndex(layer, cx, cy, cz);
        cellNeurons[layer][idx].add(neuronId);
    }

    /**
     * Returns all neurons in the same spatial cell as neuronId.
     */
    public int[] getNeighbors(int layer, int neuronId) {
        float x = neuronState.posX[neuronId];
        float y = neuronState.posY[neuronId];
        float z = neuronState.posZ[neuronId];

        int cx = (int)(x / cellSize[layer]);
        int cy = (int)(y / cellSize[layer]);
        int cz = (int)(z / cellSize[layer]);

        int idx = cellIndex(layer, cx, cy, cz);
        return cellNeurons[layer][idx].getData();
    }

    private int cellIndex(int layer, int x, int y, int z) {
        return (z * cellsY[layer] + y) * cellsX[layer] + x;
    }
}
