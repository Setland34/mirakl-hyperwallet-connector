package com.paypal.infrastructure.service;

import java.util.List;

public interface IgnoreProgramsService {

	void ignorePrograms(List<String> programs);

	List<String> getIgnoredPrograms();

	boolean isIgnored(String program);

}
