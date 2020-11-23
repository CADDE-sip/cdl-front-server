/*
    Bootstrapクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Swagger;

public class Bootstrap extends HttpServlet {

    private LogMsg log = LogMsg.getInstance();

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(log.buildLogMsg(LogLevel.DEBUG, "start"));

        Info info = new Info().title("OpenAPI Server").description("CDLv2 API").termsOfService("/terms/")
                .license(new License().name("The MIT License").url("https://opensource.org/licenses/mit-license.php"));

        Swagger swagger = new Swagger().info(info);

        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);

        System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end"));
    }
}
