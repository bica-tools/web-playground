package com.bica.web.dto;

import java.util.List;

public record GameDataResponse(
    List<GameNode> nodes,
    List<GameEdge> edges,
    int top,
    int bottom,
    int numStates,
    int numTransitions,
    String pretty
) {
    public record GameNode(int id, double x, double y, String label, String kind, boolean isTop, boolean isBottom) {}
    public record GameEdge(int src, int tgt, String label, boolean isSelection) {}
}
