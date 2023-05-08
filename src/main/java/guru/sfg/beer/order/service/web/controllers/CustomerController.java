package guru.sfg.beer.order.service.web.controllers;

import guru.sfg.beer.order.service.services.CustomerService;
import guru.sfg.brewery.model.CustomerDto;
import guru.sfg.brewery.model.CustomerPagedList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/customers")
public class CustomerController {

    private static final Integer DEFAULT_PAGE_NUMBER = 0;
    private static final Integer DEFAULT_PAGE_SIZE = 25;

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<CustomerPagedList> listCustomers(@RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                                                           @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        pageNumber = ((pageNumber == null) || (pageNumber < 0)) ? DEFAULT_PAGE_NUMBER : pageNumber;
        pageSize = ((pageSize == null) || (pageSize < 1)) ? DEFAULT_PAGE_SIZE : pageSize;
        return ResponseEntity.ok(customerService.fetchAllCustomer(PageRequest.of(pageNumber, pageSize)));

    }
}
