package com.example.tideguard.Services;


import com.example.tideguard.DTO.FloodRiskResponseDTO;

import java.util.List;

public interface FloodRiskService {
    List<FloodRiskResponseDTO> calculateFRI();
    FloodRiskResponseDTO calculateFRIForCity(String city);
}
