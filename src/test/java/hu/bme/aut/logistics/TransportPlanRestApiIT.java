package hu.bme.aut.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.web.util.UriBuilder;

import hu.bme.aut.logistics.model.TransportPlan;
import hu.bme.aut.logistics.repository.AddressRepository;
import hu.bme.aut.logistics.test.TestDataHelper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@AutoConfigureWebTestClient
public class TransportPlanRestApiIT {

    private static final String TRANSPORTPLAN_SEARCH_URI = "/transportplans";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TestDataHelper testDataHelper;

    @Autowired
    AddressRepository addressRepository;

    private List<TransportPlan> allTransportPlans;

    @BeforeEach
    public void init() {
        testDataHelper.clearDb();
        allTransportPlans = testDataHelper.insertTransportPlans();
    }

    @Test
    public void givenTransportPlansInDb_whenDateTimeIsNotSpecified_thenReturnBadRequest() throws Exception {
        searchTransportPlansAndExpectStatusIsBadRequest(null);
    }

    @Test
    public void givenTransportPlansInDb_whenIncorrectDateTimeFormatIsUsed_thenReturnBadRequest() throws Exception {
        searchTransportPlansAndExpectStatusIsBadRequest("2020/04/27 12:30");
    }

    @Test
    public void givenTransportPlansInDb_whenDateTimeIsCorrectlySpecified_thenReturnOnlyMatchingTransportPlans() throws Exception {
        List<TransportPlan> returnedTransportPlans = searchTransportPlans("2020-03-01T10-00-00");
        assertThat(returnedTransportPlans).hasSize(1);
        assertThat(returnedTransportPlans.get(0).getId()).isEqualTo(allTransportPlans.get(1).getId());
    }

    private List<TransportPlan> searchTransportPlans(String dateTime) {
        return buildSearchTransportPlanRequest(dateTime)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TransportPlan>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private void searchTransportPlansAndExpectStatusIsBadRequest(String dateTime) {
        buildSearchTransportPlanRequest(dateTime)
                .expectStatus().isBadRequest();
    }

    private ResponseSpec buildSearchTransportPlanRequest(String dateTime) {
        WebTestClient.RequestHeadersSpec<?> uri = webTestClient
                .get()
                .uri(builder -> {
                    UriBuilder path = builder.path(TRANSPORTPLAN_SEARCH_URI);
                    if (dateTime != null)
                        path = path.queryParam("dateTime", dateTime);
                    return path.build();
                });


        return uri.exchange();
    }

}
