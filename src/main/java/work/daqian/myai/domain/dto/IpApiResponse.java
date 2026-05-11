package work.daqian.myai.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class IpApiResponse {

    @JsonProperty("query")
    private String query;
    @JsonProperty("status")
    private String status;
    @JsonProperty("country")
    private String country;
    @JsonProperty("countryCode")
    private String countryCode;
    @JsonProperty("region")
    private String region;
    @JsonProperty("regionName")
    private String regionName;
    @JsonProperty("city")
    private String city;
    @JsonProperty("zip")
    private String zip;
    @JsonProperty("lat")
    private Double lat;
    @JsonProperty("lon")
    private Double lon;
    @JsonProperty("timezone")
    private String timezone;
    @JsonProperty("isp")
    private String isp;
    @JsonProperty("org")
    private String org;
    @JsonProperty("as")
    private String as;
}
