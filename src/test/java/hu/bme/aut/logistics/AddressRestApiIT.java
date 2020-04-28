package hu.bme.aut.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.web.util.UriBuilder;

import hu.bme.aut.logistics.model.Address;
import hu.bme.aut.logistics.repository.AddressRepository;
import hu.bme.aut.logistics.test.TestDataHelper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@AutoConfigureWebTestClient
public class AddressRestApiIT {

    private static final String ADDRESSES_URI = "/addresses";
    private static final String ADDRESS_WITH_ID_URI = ADDRESSES_URI + "/{id}";
    private static final String ADDRESS_SEARCH_URI = ADDRESSES_URI + "/search";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TestDataHelper testDataHelper;

    @Autowired
    AddressRepository addressRepository;

    @BeforeEach
    public void init() {
        testDataHelper.clearDb();
    }


    /***TEST CASES FOR GET****/

    @Test
    public void givenNoAddresses_whenGetAddresses_thenReturnEmptyList()
            throws Exception {

        List<Address> foundAddresses = getAllAddresses();

        assertThat(foundAddresses).isEmpty();
    }

    @Test
    public void given2Addresses_whenGetAddresses_thenReturnListOf2()
            throws Exception {
        Address address1 = testDataHelper.insertTestAddress();
        Address address2 = testDataHelper.insertTestAddress();

        List<Address> foundAddresses = getAllAddresses();

        assertThat(foundAddresses).hasSize(2);
        assertAddress(foundAddresses, address1);
        assertAddress(foundAddresses, address2);
    }

    @Test
    public void givenOneAddress_whenGetAddressByOtherId_thenReturn404()
            throws Exception {

        testDataHelper.insertTestAddress();

        buildGetAddressRequest(-1L)
                .expectStatus().isNotFound();
    }

    @Test
    public void given2Addresses_whenGetAddressById_thenReturnThatOneAddress()
            throws Exception {

        Address address1 = testDataHelper.insertTestAddress();
        testDataHelper.insertTestAddress();

        Address foundAddress = getAddressById(address1.getId());

        assertThat(foundAddress).isEqualToComparingFieldByField(address1);
    }

    /***TEST CASES FOR POST****/

    @Test
    public void givenNoAddresses_whenPostNewAddress_thenAddressWithReturnedIdIsInserted()
            throws Exception {

        Address newAddress = testDataHelper.createTestAddress();
        Long newId = buildPostAddressRequest(newAddress)
                .expectStatus().isOk()
                .expectBody(Long.class)
                .returnResult()
                .getResponseBody();

        newAddress.setId(newId);

        assertThat(addressRepository.findById(newId).get()).isEqualToComparingFieldByField(newAddress);
    }

    @Test
    public void givenEmptyLatAndLong_whenPostNewAddress_thenAddressWithReturnedIdIsInserted()
            throws Exception {

        Address newAddress = testDataHelper.createTestAddress();
        newAddress.setGeoLat(null);
        newAddress.setGeoLng(null);
        Long newId = buildPostAddressRequest(newAddress)
                .expectStatus().isOk()
                .expectBody(Long.class)
                .returnResult()
                .getResponseBody();

        newAddress.setId(newId);

        assertThat(addressRepository.findById(newId).get()).isEqualToComparingFieldByField(newAddress);
    }

    @Test
    public void whenPostAddressWithoutBody_thenReturnBadRequest()
            throws Exception {
        postAddressAndExpectStatusIsBadRequest(null);
    }


    @Test
    public void whenPostAddressWithIdFilled_thenReturnBadRequest()
            throws Exception {

        Address address = testDataHelper.createTestAddress();
        address.setId(100L);
        postAddressAndExpectStatusIsBadRequest(address);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number"})
    public void whenPostAddressWithNullOrEmptyProperty_thenReturnBadRequest(String prop)
            throws Exception {

        Address address = testDataHelper.createTestAddress();

        BeanUtils.setProperty(address, prop, null);
        postAddressAndExpectStatusIsBadRequest(address);
        BeanUtils.setProperty(address, prop, "");
        postAddressAndExpectStatusIsBadRequest(address);
    }

    /***TEST CASES FOR DELETE****/

    @Test
    public void whenDeleteAddress_thenAddressIsDeletedFromDb()
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();

        deleteAddress(id);
        assertThat(addressRepository.findById(id)).isEmpty();
    }

    @Test
    public void whenDeleteAddressWithNonExistingId_thenNothingIsDeleted()
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();
        deleteAddress(id + 1);
        assertThat(addressRepository.findById(id)).isNotEmpty();
    }

    /***TEST CASES FOR PUT****/

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number", "geoLng", "geoLat"})
    public void givenOneAddress_whenPutAddressWithModifiedPropertyAndSameIdInBody_thenPropertyIsModifiedInDb(String propToChange)
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();

        Object value = getModifiedPropertyValue(address, propToChange);
        BeanUtils.setProperty(address, propToChange, value);

        Address returnedAddress = putAddress(id, address);
        assertThat(returnedAddress).isEqualToComparingFieldByField(address);
        assertThat(addressRepository.findById(id).get()).isEqualToComparingFieldByField(address);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number", "geoLng", "geoLat"})
    public void givenOneAddress_whenPutAddressWithModifiedPropertyAndNoIdInBody_thenPropertyIsModifiedInDb(String propToChange)
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();
        address.setId(null);

        Object value = getModifiedPropertyValue(address, propToChange);
        BeanUtils.setProperty(address, propToChange, value);

        Address returnedAddress = putAddress(id, address);
        address.setId(id);
        assertThat(returnedAddress).isEqualToComparingFieldByField(address);
        assertThat(addressRepository.findById(id).get()).isEqualToComparingFieldByField(address);
    }

    @Test
    public void givenNoAddress_whenPutAddressWithId_thenStatusNotFoundAndNothingIsInserted()
            throws Exception {

        Address address = testDataHelper.createTestAddress();
        Long id = 100L;

        buildPutRequest(id, address)
                .expectStatus().isNotFound();

        assertThat(addressRepository.findById(id)).isEmpty();
    }

    @Test
    public void whenPutAddressWithNoBody_thenReturnBadRequest()
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();

        putAddressAndExpectStatusIsBadRequest(id, null);
    }

    @Test
    public void whenPutAddressWithDifferentIdInBody_thenReturnBadRequest()
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();

        address.setId(id + 1);

        putAddressAndExpectStatusIsBadRequest(id, address);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number"})
    public void whenPutAddressWithNullOrEmptyProperty_thenReturnBadRequest(String prop)
            throws Exception {
        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();

        BeanUtils.setProperty(address, prop, null);
        putAddressAndExpectStatusIsBadRequest(id, address);
        BeanUtils.setProperty(address, prop, "");
        putAddressAndExpectStatusIsBadRequest(id, address);
    }

    @Test
    public void givenEmptyLatAndLong_whenPutAddress_thenAddressIsModified()
            throws Exception {

        Address address = testDataHelper.insertTestAddress();
        Long id = address.getId();
        address.setGeoLat(null);
        address.setGeoLng(null);

        Address returnedAddress = putAddress(id, address);
        assertThat(returnedAddress).isEqualToComparingFieldByField(address);
        assertThat(addressRepository.findById(id).get()).isEqualToComparingFieldByField(address);
    }


    /** TEST CASES FOR SEARCH **/

    /**
     * 2.a: TEST CASES WITH NO PAGING
     **/

    @Test
    public void whenSearchWithoutBody_thenReturnBadRequest()
            throws Exception {
        searchAndExpectStatusIsBadRequest(null);
    }

    @Test
    public void givenNoAddressesInDb_whenSearch_thenReturnEmptyList()
            throws Exception {

        Address example = testDataHelper.createTestAddress();
        List<Address> foundAddresses = searchAddresses(example);
        assertThat(foundAddresses).isEmpty();
    }

    @Test
    public void givenAddressesInDb_whenSearchByUnsupportedProperties_thenReturnAllAddresses()
            throws Exception {

        Address address1 = testDataHelper.insertTestAddress();
        Address address2 = testDataHelper.insertTestAddress();
        Address example = testDataHelper.createTestAddress();
        example.setCity(null);
        example.setCountry(null);
        example.setStreet(null);
        example.setGeoLat(example.getGeoLat() + 10);
        example.setGeoLng(example.getGeoLng() + 10);
        example.setId(-1L);
        example.setNumber("nomatch");
        example.setZipCode("nomatch");

        List<Address> foundAddresses = searchAddresses(example);
        assertThat(foundAddresses).hasSize(2);
        assertAddress(foundAddresses, address1);
        assertAddress(foundAddresses, address2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street"})
    public void givenAddressesInDb_whenSearchByExactProperty_thenReturnMatchingAddresses(String prop)
            throws Exception {
        Address address1 = testDataHelper.insertTestAddress();
        String searchValue = BeanUtils.getProperty(address1, prop);
        testSearchByPropWithValue(address1, prop, searchValue, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street"})
    public void givenAddressesInDb_whenSearchByExactPropertyWithDifferentCase_thenReturnMatchingAddresses(String prop)
            throws Exception {
        Address address1 = testDataHelper.insertTestAddress();
        String searchValue = BeanUtils.getProperty(address1, prop).toUpperCase();
        testSearchByPropWithValue(address1, prop, searchValue, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street"})
    public void givenAddressesInDb_whenSearchByPropertyPrefix_thenReturnMatchingAddresses(String prop)
            throws Exception {
        Address address1 = testDataHelper.insertTestAddress();
        String searchValue = BeanUtils.getProperty(address1, prop).substring(0, 1);
        testSearchByPropWithValue(address1, prop, searchValue, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street"})
    public void givenAddressesInDb_whenSearchByPropertyMiddleValue_thenReturnEmptyList(String prop)
            throws Exception {
        Address address1 = testDataHelper.insertTestAddress();
        String searchValue = BeanUtils.getProperty(address1, prop).substring(1, 2);
        testSearchByPropWithValue(address1, prop, searchValue, 0);
    }

    @Test
    public void givenAddressesInDb_whenSearchByAllSupportedProperties_thenReturnAddressesWithAllPropertiesMatching() {
        Address address1 = testDataHelper.insertTestAddress();
        Address address2 = testDataHelper.insertTestAddress();

        Address address3 = testDataHelper.createTestAddress();
        address3.setCity("nomatch");
        address3 = testDataHelper.insertTestAddress(address3);

        Address address4 = testDataHelper.createTestAddress();
        address4.setCountry("nomatch");
        address4 = testDataHelper.insertTestAddress(address4);

        Address address5 = testDataHelper.createTestAddress();
        address5.setStreet("nomatch");
        address5 = testDataHelper.insertTestAddress(address5);

        List<Address> foundAddresses = searchAddresses(address1);
        assertThat(foundAddresses).hasSize(2);
        assertAddress(foundAddresses, address1);
        assertAddress(foundAddresses, address2);
    }

    /**
     * 2.b: TEST CASES WITH PAGING
     **/

    @Test
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithPaging_thenReturnProperPageOfMatchingAddresses() throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentNumbers(21);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = matchingAddresses.get(0);
        List<Address> foundPage = searchAddresses(example, 0, 10, "id");

        assertThat(foundPage).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(i));
        }

        foundPage = searchAddresses(example, 1, 10, "id");
        assertThat(foundPage).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(10 + i));
        }

        foundPage = searchAddresses(example, 2, 10, "id");
        assertThat(foundPage).hasSize(1);
        assertThat(foundPage.get(0)).isEqualToComparingFieldByField(matchingAddresses.get(20));
    }

    @Test
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithPaging_thenReturnXTotalCountHeader() throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentNumbers(21);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = matchingAddresses.get(0);

        buildSearchAddressRequest(example, 0, 10, null)
                .expectHeader().valueEquals("X-Total-Count", String.valueOf(21));

        buildSearchAddressRequest(example, 1, 10, null)
                .expectHeader().valueEquals("X-Total-Count", String.valueOf(21));

        buildSearchAddressRequest(example, 2, 10, null)
                .expectHeader().valueEquals("X-Total-Count", String.valueOf(21));

    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number", "geoLat", "geoLng"})
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithSortWithoutDirection_thenReturnMatchingAddressesInProperOrder(String sortProp) throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentProps(9, sortProp);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = testDataHelper.createTestAddress();

        List<Address> foundPage = searchAddresses(example, 0, 4, sortProp);

        assertThat(foundPage).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(8 - i));
        }

        foundPage = searchAddresses(example, 1, 4, sortProp);
        assertThat(foundPage).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(4 - i));
        }

        foundPage = searchAddresses(example, 2, 4, sortProp);
        assertThat(foundPage).hasSize(1);
        assertThat(foundPage.get(0)).isEqualToComparingFieldByField(matchingAddresses.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number", "geoLat", "geoLng"})
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithSortAsc_thenReturnMatchingAddressesInProperOrder(String sortProp) throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentProps(9, sortProp);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = testDataHelper.createTestAddress();

        String sortParam = sortProp + ",asc";
        List<Address> foundPage = searchAddresses(example, 0, 4, sortParam);

        assertThat(foundPage).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(8 - i));
        }

        foundPage = searchAddresses(example, 1, 4, sortParam);
        assertThat(foundPage).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(4 - i));
        }

        foundPage = searchAddresses(example, 2, 4, sortParam);
        assertThat(foundPage).hasSize(1);
        assertThat(foundPage.get(0)).isEqualToComparingFieldByField(matchingAddresses.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"country", "city", "street", "zipCode", "number", "geoLat", "geoLng"})
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithSortDesc_thenReturnMatchingAddressesInProperOrder(String sortProp) throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentProps(9, sortProp);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = testDataHelper.createTestAddress();

        String sortParam = sortProp + ",desc";
        List<Address> foundPage = searchAddresses(example, 0, 4, sortParam);

        assertThat(foundPage).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(i));
        }

        foundPage = searchAddresses(example, 1, 4, sortParam);
        assertThat(foundPage).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(4 + i));
        }

        foundPage = searchAddresses(example, 2, 4, sortParam);
        assertThat(foundPage).hasSize(1);
        assertThat(foundPage.get(0)).isEqualToComparingFieldByField(matchingAddresses.get(8));
    }

    @Test
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithNoSize_thenReturnAllMatchingAddresses() throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentNumbers(21);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = matchingAddresses.get(0);
        List<Address> foundPage = searchAddresses(example, 0, null, "id");

        assertThat(foundPage).hasSize(21);
        for (int i = 0; i < 21; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(i));
        }
    }

    @Test
    public void givenAddressesInDb_whenSearchByAllSupportedPropertiesWithNoPage_thenReturnFirstPageOfMathcingAddresses() throws Exception {

        List<Address> matchingAddresses = insertAddressesWithDifferentNumbers(21);

        Address nonMatching = testDataHelper.createTestAddress();
        nonMatching.setCity("nomatch");
        testDataHelper.insertTestAddress(nonMatching);

        Address example = matchingAddresses.get(0);
        List<Address> foundPage = searchAddresses(example, null, 10, "id");

        assertThat(foundPage).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(foundPage.get(i)).isEqualToComparingFieldByField(matchingAddresses.get(i));
        }
    }

    
    private List<Address> insertAddressesWithDifferentNumbers(int count) throws Exception {
        return insertAddressesWithDifferentProps(count, "number");
    }


    private List<Address> insertAddressesWithDifferentProps(int count, String prop) throws Exception {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Address address = testDataHelper.createTestAddress();
            BeanUtils.setProperty(address, prop, BeanUtils.getProperty(address, prop) + (count - i));
            addresses.add(testDataHelper.insertTestAddress(address));
        }
        return addresses;
    }

    private void testSearchByPropWithValue(Address address1, String prop, String searchValue, int expectedCount)
            throws IllegalAccessException, InvocationTargetException {

        Address address2 = testDataHelper.insertTestAddress();
        Address address3 = testDataHelper.createTestAddress();
        BeanUtils.setProperty(address3, prop, "nomatch");
        address3 = testDataHelper.insertTestAddress(address3);

        Address example = new Address();
        BeanUtils.setProperty(example, prop, searchValue);

        List<Address> foundAddresses = searchAddresses(example);
        assertThat(foundAddresses).hasSize(expectedCount);
        if (expectedCount > 0) {
            assertAddress(foundAddresses, address1);
            assertAddress(foundAddresses, address2);
        }
    }


    private List<Address> getAllAddresses() {
        return webTestClient.get()
                .uri(ADDRESSES_URI)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Address>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private Address getAddressById(Long id) {
        return buildGetAddressRequest(id)
                .expectStatus().isOk()
                .expectBody(Address.class)
                .returnResult()
                .getResponseBody();
    }

    private ResponseSpec buildGetAddressRequest(Long id) {
        return webTestClient.get()
                .uri(builder -> builder.path(ADDRESS_WITH_ID_URI).build(id))
                .exchange();
    }

    private void deleteAddress(Long id) {
        webTestClient.delete()
                .uri(builder -> builder.path(ADDRESS_WITH_ID_URI).build(id))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    private void postAddressAndExpectStatusIsBadRequest(Address address) {
        buildPostAddressRequest(address)
                .expectStatus().isBadRequest();
    }

    private ResponseSpec buildPostAddressRequest(Address address) {
        RequestBodySpec uri = webTestClient
                .post()
                .uri(ADDRESSES_URI);

        if (address != null)
            return uri.bodyValue(address).exchange();
        else
            return uri.exchange();
    }

    private Address putAddress(Long id, Address address) {
        return buildPutRequest(id, address)
                .expectStatus().isOk()
                .expectBody(Address.class)
                .returnResult()
                .getResponseBody();
    }

    private ResponseSpec buildPutRequest(Long id, Address address) {
        RequestBodySpec uri = webTestClient
                .put()
                .uri(builder -> builder.path(ADDRESS_WITH_ID_URI).build(id));
        if (address != null)
            return uri.bodyValue(address).exchange();
        else
            return uri.exchange();
    }


    private void assertAddress(List<Address> foundAddresses, Address address) {
        assertThat(foundAddresses.stream()
                .filter(a -> a.getId().equals(address.getId()))
                .findFirst()
                .get())
                .isEqualToComparingFieldByField(address);
    }

    private Object getModifiedPropertyValue(Address address, String prop) throws Exception {
        String currentValue = BeanUtils.getProperty(address, prop);
        if (prop.equals("geoLat") || prop.equals("geoLng")) {
            return Double.parseDouble(currentValue) + 1.0;
        } else {
            return currentValue + "modified";
        }
    }

    private void putAddressAndExpectStatusIsBadRequest(Long id, Address address) {
        buildPutRequest(id, address)
                .expectStatus().isBadRequest();
    }

    private List<Address> searchAddresses(Address address) {
        return buildSearchAddressRequest(address)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Address>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private List<Address> searchAddresses(Address address, Integer page, Integer size, String sort) {
        return buildSearchAddressRequest(address, page, size, sort)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Address>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private void searchAndExpectStatusIsBadRequest(Address address) {
        buildSearchAddressRequest(address)
                .expectStatus().isBadRequest();
    }

    private ResponseSpec buildSearchAddressRequest(Address address, Integer page, Integer size, String sort) {
        RequestBodySpec uri = webTestClient
                .post()
                .uri(builder -> {
                    UriBuilder path = builder.path(ADDRESS_SEARCH_URI);
                    if (page != null)
                        path = path.queryParam("page", page);
                    if (size != null)
                        path = path.queryParam("size", size);
                    if (sort != null)
                        path = path.queryParam("sort", sort);
                    return path.build();
                });

        if (address != null)
            return uri.bodyValue(address).exchange();
        else
            return uri.exchange();
    }

    private ResponseSpec buildSearchAddressRequest(Address address) {
        return buildSearchAddressRequest(address, null, null, null);
    }

}
