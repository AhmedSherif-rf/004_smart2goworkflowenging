package com.ntg.engine;

import com.ntg.common.STAGESystemOut;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		STAGESystemOut.OverrideSystemOutput();


		SpringApplicationBuilder re = application.sources(Application.class);
		Application.NTGInit();

		return re;
	}

}
