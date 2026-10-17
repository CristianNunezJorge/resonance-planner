package com.resonanceplanner.agent.tools;

import com.resonanceplanner.catalog.CatalogService;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.planner.PlannerService;
import com.resonanceplanner.planning.CalendarService;
import com.resonanceplanner.planning.ProposalService;
import com.resonanceplanner.roster.RosterService;
import com.resonanceplanner.user.UserService;
import com.resonanceplanner.waveplate.WaveplateService;
import org.springframework.stereotype.Component;

/**
 * The services the tools call: the same ones as the API, so a tool checks and limits what an endpoint does (D55).
 * Every method that reads data of a user takes the user of the turn.
 */
@Component
public record AgentToolServices(
        RosterService rosterService,
        CatalogService catalogService,
        WaveplateService waveplateService,
        UserService userService,
        EventService eventService,
        CalendarService calendarService,
        PlannerService plannerService,
        ProposalService proposalService) {}
