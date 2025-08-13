package a2a.example.agents.flight.controller;


import java.util.List;

import a2a.example.agents.flight.services.BookingTools;
import a2a.example.agents.flight.services.FlightBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/")
public class BookingController {

	@Autowired
	private FlightBookingService flightBookingService;


	@RequestMapping("/api/bookings")
	@ResponseBody
	public List<BookingTools.BookingDetails> getBookings() {
		return flightBookingService.getBookings();
	}

}
