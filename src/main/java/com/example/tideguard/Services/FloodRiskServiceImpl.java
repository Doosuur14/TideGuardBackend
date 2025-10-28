package com.example.tideguard.Services;

import com.example.tideguard.DTO.FloodRiskResponseDTO;
import com.example.tideguard.DTO.ShelterDTO;
import com.example.tideguard.DTO.SoilAndRainData;
import com.example.tideguard.Models.Shelters;
import com.example.tideguard.Repositories.ShelterRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FloodRiskServiceImpl implements FloodRiskService {

    @Autowired
    private ShelterRepository shelterRepository;
    @Autowired
    private SoilAndRainService soilAndRainService;
    @Override
    public List<FloodRiskResponseDTO> calculateFRI() {
        List<FloodRiskResponseDTO> responses = new ArrayList<>();
        try {
            // 1️⃣ Load historical flood data
            ClassPathResource floodResource = new ClassPathResource("Nigeria_Flood_Events_2010_2024.json");
            InputStream floodStream = floodResource.getInputStream();
            String floodJson = new String(floodStream.readAllBytes(), StandardCharsets.UTF_8);
            JSONArray yearlyData = new JSONArray(floodJson);

            for (int i = 0; i < yearlyData.length(); i++) {
                JSONObject yearObj = yearlyData.getJSONObject(i);
                JSONArray eventsArray = yearObj.getJSONArray("events");

                // 3️⃣ Loop through each event (state)
                for (int j = 0; j < eventsArray.length(); j++) {
                    JSONObject eventObj = eventsArray.getJSONObject(j);
                    FloodRiskResponseDTO response = new FloodRiskResponseDTO();

                    String city = eventObj.getString("state");
                    response.setCity(city);

                    // 4️⃣ Extract coordinates directly from JSON
                    double latitude = eventObj.getDouble("latitude");
                    double longitude = eventObj.getDouble("longitude");

                    // 5️⃣ Fetch rainfall + soil data dynamically
                    SoilAndRainData soilAndRainData = soilAndRainService.getSoilAndRainData(latitude, longitude);
                    double rainfall = soilAndRainData.getRainfall();
                    double soilSaturation = soilAndRainData.getSoilSaturation();

                    // 6️⃣ Compute simple historical flood frequency score
                    // You can refine this part later if you want yearly averages.
                    double floodHistory = eventObj.getDouble("people_affected") / 500000.0; // normalized factor

                    // 7️⃣ Compute Flood Risk Index (FRI)
                    double fri = (0.4 * (rainfall / 60)) + (0.3 * soilSaturation) + (0.3 * floodHistory);
                    response.setFri(fri);
                    response.setRainfall(rainfall);
                    response.setSoilSaturation(soilSaturation);
                    response.setFloodHistory(floodHistory);

                    // 8️⃣ Determine risk level
                    if (fri < 0.4) response.setRiskLevel("LOW");
                    else if (fri < 0.7) response.setRiskLevel("MEDIUM");
                    else response.setRiskLevel("HIGH");

                    // 9️⃣ Fetch shelters for city
                    List<Shelters> dbShelters = shelterRepository.findByCity(city);
                    List<ShelterDTO> shelterDTOs = new ArrayList<>();
                    for (Shelters s : dbShelters) {
                        ShelterDTO dto = new ShelterDTO();
                        dto.setName(s.getName());
                        dto.setAddress(s.getAddress());
                        dto.setLatitude(s.getLatitude());
                        dto.setLongitude(s.getLongitude());
                        dto.setRouteDescription(s.getRouteDescription());
                        shelterDTOs.add(dto);
                    }
                    response.setShelters(shelterDTOs);

                    responses.add(response);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return responses;
    }

    @Override
    public FloodRiskResponseDTO calculateFRIForCity(String city) {
        return calculateFRI().stream()
                .filter(r -> r.getCity().equalsIgnoreCase(city))
                .findFirst()
                .orElse(null);
    }
}
