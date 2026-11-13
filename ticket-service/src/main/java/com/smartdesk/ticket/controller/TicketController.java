package com.smartdesk.ticket.controller;

import com.smartdesk.ticket.dto.CreateTicketRequest;
import com.smartdesk.ticket.dto.TicketResponse;
import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.mapper.TicketMapper;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.service.CreateResult;
import com.smartdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService service;
    private final TicketMapper mapper;

    /** 201 = new ticket; 200 = same Idempotency-Key seen before, original ticket returned. */
    @PostMapping
    public ResponseEntity<TicketResponse> create(Caller c, @Valid @RequestBody CreateTicketRequest req,
                                                 @RequestHeader(name = "Idempotency-Key", required = false) @Size(max = 100) String key) {
        CreateResult result = service.create(c, req, key);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(mapper.toResponse(result.ticket()));
    }

    @GetMapping
    public Page<TicketResponse> list(Caller c,
                                     @RequestParam(defaultValue = "false") boolean mine,
                                     @RequestParam(required = false) TicketStatus status,
                                     @RequestParam(required = false) Department category,
                                     @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
        return service.list(c, mine, status, category, page).map(mapper::toResponse);
    }

    @GetMapping("/pool")
    public Page<TicketResponse> pool(Caller c, @PageableDefault(sort = "createdAt") Pageable page) {
        return service.pool(c, page).map(mapper::toResponse);
    }

    @GetMapping("/assigned")
    public Page<TicketResponse> assigned(Caller c, @PageableDefault(sort = "createdAt") Pageable page) {
        return service.assignedToMe(c, page).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public TicketResponse get(Caller c, @PathVariable Long id) {
        return mapper.toResponse(service.get(c, id));
    }
}
