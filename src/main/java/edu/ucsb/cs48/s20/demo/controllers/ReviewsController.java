package edu.ucsb.cs48.s20.demo.controllers;

import edu.ucsb.cs48.s20.demo.advice.StudentFlowAdvice;
import edu.ucsb.cs48.s20.demo.entities.ProjectIdea;
import edu.ucsb.cs48.s20.demo.entities.Review;
import edu.ucsb.cs48.s20.demo.entities.Student;
import edu.ucsb.cs48.s20.demo.formbeans.Idea;
import edu.ucsb.cs48.s20.demo.formbeans.ReviewBean;
import edu.ucsb.cs48.s20.demo.repositories.ProjectIdeaRepository;
import edu.ucsb.cs48.s20.demo.repositories.ReviewRepository;
import edu.ucsb.cs48.s20.demo.repositories.StudentRepository;
import edu.ucsb.cs48.s20.demo.services.CSVToObjectService;
import edu.ucsb.cs48.s20.demo.services.MembershipService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.IllegalSelectorException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewsController {

    private Logger logger = LoggerFactory.getLogger(ReviewsController.class);

    @Autowired
    private MembershipService ms;

    @Autowired
    private StudentFlowAdvice studentFlowAdvice;

    private StudentRepository studentRepository;

    private ProjectIdeaRepository projectIdeaRepository;

    private ReviewRepository reviewRepository;

   

    @Autowired
    public ReviewsController(ProjectIdeaRepository projectIdeaRepository, StudentRepository studentRepository, ReviewRepository reviewRepository) {
        this.studentRepository = studentRepository;
        this.projectIdeaRepository = projectIdeaRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/reviews")
    public String reviews(Model model, OAuth2AuthenticationToken token, RedirectAttributes redirAttrs) {
        String role = ms.role(token);
        if (!role.equals("Admin")) {
            redirAttrs.addFlashAttribute("alertDanger", "You do not have permission to access that page");
            return "redirect:/";
        }
        model.addAttribute("reviews", reviewRepository.findAll());
        return "reviews/index";
    }

    @PostMapping("/reviews/delete/{id}")
    public String deleteIdea(@PathVariable("id") Long id, Model model, RedirectAttributes redirAttrs,
            OAuth2AuthenticationToken token) {
        String role = ms.role(token);
        if (!role.equals("Admin")) {
            redirAttrs.addFlashAttribute("alertDanger", "You do not have permission to access that page");
            return "redirect:/";
        }

        Optional<Review> optionalReview = reviewRepository.findById(id);
        if (!optionalReview.isPresent()) {
            redirAttrs.addFlashAttribute("alertDanger", "Review with id " + id + " does not exist.");
        } else {
            Review review = optionalReview.get();
            reviewRepository.delete(review);
            redirAttrs.addFlashAttribute("alertSuccess",
                    "Review  " + id + " successfully deleted.");
        }
        model.addAttribute("reviews", reviewRepository.findAll());
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/add")
    public String addReview(@ModelAttribute("review") ReviewBean reviewBean, Model model, OAuth2AuthenticationToken token,
            BindingResult bindingResult, RedirectAttributes redirAttrs) {
        String role = ms.role(token);
        if (!role.equals("Admin") && !role.equals("Student")) {
            redirAttrs.addFlashAttribute("alertDanger", "You do not have permission to access that page");
            return "redirect:/";
        }

        boolean errors =false;

        model.addAttribute("ratingHasErrors", false);
        model.addAttribute("detailHasErrors", false);

        if (reviewBean.getRating() == null ) {
            model.addAttribute("ratingErrors", "You must select a rating");
            model.addAttribute("ratingHasErrors", true);
            errors = true;

        }

        if (reviewBean.getDetails() == null || reviewBean.getDetails().length() < 20) {
            model.addAttribute("detailErrors", "Please add some more detail");
            model.addAttribute("detailHasErrors", true);
            errors = true;
        }

        if (!errors) {
            Review review = new Review();
            Student reviewer = studentFlowAdvice.getStudent(token);
            Long ideaId = review.getIdea().getId();
            Optional<ProjectIdea> optionalProjectIdea = projectIdeaRepository.findById(review.getIdea().getId());
            if (!optionalProjectIdea.isPresent()) {
                throw new IllegalStateException("Trying to create a review for non existing idea with id "+ ideaId);
            }
            ProjectIdea projectIdea = optionalProjectIdea.get();

            review.setReviewer(reviewer);
            review.setIdea(projectIdea);
            review.setRating(reviewBean.getRating());
            review.setDetails(reviewBean.getDetails());
            reviewRepository.save(review);
            return "redirect:/";

        }

        model.addAttribute("review", reviewBean);
    
        logger.info("leaving ReviewController addReview:");
        logger.info("review"+reviewBean);

        return "index";
        
    }

}