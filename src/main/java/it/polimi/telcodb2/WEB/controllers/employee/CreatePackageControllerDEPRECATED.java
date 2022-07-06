package it.polimi.telcodb2.WEB.controllers.employee;

import it.polimi.telcodb2.EJB.entities.Package;
import it.polimi.telcodb2.EJB.services.PackageService;
import it.polimi.telcodb2.EJB.services.ProductService;
import it.polimi.telcodb2.EJB.services.ServiceService;
import it.polimi.telcodb2.EJB.services.ValidityService;
import it.polimi.telcodb2.EJB.utils.Pair;
import it.polimi.telcodb2.EJB.utils.ParseUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "CreatePackage", value="/create-package-deprecated")
public class CreatePackageControllerDEPRECATED extends HttpServlet {

    @EJB
    private PackageService packageService;
    @EJB
    private ProductService productService;
    @EJB
    private ServiceService serviceService;
    @EJB
    private ValidityService validityService;


    public CreatePackageControllerDEPRECATED() {
        super();
    }

    public void init() {
        ServletContext servletContext = getServletContext();
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setSuffix(".html");
    }


    // GET requests are not allowed
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "GET is not allowed");
    }


    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Check if session is valid
        HttpSession session = request.getSession();
        if (session.getAttribute("username") == null || session.getAttribute("userid") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid sessions");
        }

        // Parse name
        String name = StringEscapeUtils.escapeJava(request.getParameter("name"));
        // Parse validity options
        List<Pair<Integer, Float>> validityOptions = Pair.createPairList(
                ParseUtils.toIntegerList(
                        ParseUtils.toStringListSafe(request.getParameterValues("duration"))
                                .stream()
                                .filter(elem -> !elem.isEmpty())
                                .collect(Collectors.toList()),
                        true),
                ParseUtils.toFloatList(
                        ParseUtils.toStringListSafe(request.getParameterValues("fee"))
                                .stream()
                                .filter(elem -> !elem.isEmpty())
                                .collect(Collectors.toList()),
                        true)
        );
        // Parse services
        List<Integer> serviceIds = ParseUtils.toIntegerList(
                ParseUtils.toStringListSafe(request.getParameterValues("services")), false);
        // Parse optional products
        List<Integer> productIds = ParseUtils.toIntegerList(
                ParseUtils.toStringListSafe(request.getParameterValues("products")), false);

        //  Check empty fields
        if (name.isEmpty()) {
            response.sendRedirect(getServletContext().getContextPath() + "/employee-home?error=No name was submitted");
            return;
        }
        if (validityOptions == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/employee-home?error=No validity was submitted");
            return;
        }
        if (serviceIds.isEmpty()) {
            response.sendRedirect(getServletContext().getContextPath() + "/employee-home?error=No service was submitted");
            return;
        }

        // Check if there is already a package with the same name
        if (!packageService.findByName(name).isEmpty()) {
            response.sendRedirect(getServletContext().getContextPath() + "/employee-home?error=Already existing package");
            return;
        }

        // Check if there are validity options with the same duration
        if (validityOptions.stream().map(Pair::getX).distinct().count() != validityOptions.size()) {
            response.sendRedirect(getServletContext().getContextPath() + "/employee-home?error=Repeated duration in validity options");
            return;
        }

        // Create the list of compatible validities
//        List<Integer> validityIds = new ArrayList<Integer>();
//        validityOptions.forEach(validityOption -> {
//            List<Validity> tmpValidityList = validityService.findEquivalents(validityOption.getX(), validityOption.getY());
//            // Create a new validity and add it to the validity list if tre is not already an equivalent one
//            if (tmpValidityList.isEmpty()) {
//                validityIds.add(validityService.createValidity(validityOption.getX(), validityOption.getY()).getIdValidity());
//            }
//            // Add an already existing equivalent validity to the validity list
//            else {
//                validityIds.add(tmpValidityList.get(0).getIdValidity());
//            }
//        });

        // Create package
        Package newPackage = packageService.createPackage(name, validityOptions, serviceIds, productIds);
        if (newPackage == null) {
            response.sendRedirect(getServletContext().getContextPath() + "/employee-home?error=Ops! Something went wrong");
            return;
        }
        response.sendRedirect(getServletContext().getContextPath() + "/employee-home?success=true");
    }
}
