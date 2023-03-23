package com.mysite.sbb.question;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.answer.AnswerForm;
import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;


@RequestMapping("/question")
@RequiredArgsConstructor
@Controller
public class QuestionController {
    private final QuestionService questionService;
    private final UserService userService;
    private final AnswerService answerService;

    private SiteUser getSiteUser(Principal principal) {
        SiteUser siteUser = null;
        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2User oAuth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            String email = oAuth2User.getAttribute("email");
            siteUser = this.userService.findByEmail(email);
            siteUser.setGoogleId(email);
        } else if(principal instanceof UserDetails) {
            siteUser = this.userService.getUser(principal.getName());
        }
        return siteUser;
    }

    @GetMapping("/list")
    public String list(Model model, @RequestParam(value="page", defaultValue="1") int page,    @RequestParam(value = "kw", defaultValue = "") String kw) {
        int pageSize=10;
        Page<Question> paging = this.questionService.getList(page,pageSize,kw);
        model.addAttribute("paging", paging);
        model.addAttribute("pageSize",pageSize);
        model.addAttribute("kw", kw);
        return "question_list";
    }

    @GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Integer id,  @RequestParam(value = "page", defaultValue = "1")int page, AnswerForm answerForm,Principal principal) {
        Question question = this.questionService.getQuestion(id);
        int pageSize = 5;
        Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Answer> paging = question.getAnswers(pageable);
        System.out.println(paging.getSize());
        model.addAttribute("question", question);
        model.addAttribute("paging",paging);
        model.addAttribute("pageSize",pageSize);
        SiteUser siteUser = getSiteUser(principal);
        model.addAttribute("siteUser",siteUser);
        return "question_detail";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/create")
    public String questionCreate(QuestionForm questionForm){
        return "question_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    public String questionCreate(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        SiteUser siteUser = getSiteUser(principal);
        Question q = this.questionService.create(questionForm.getSubject(), questionForm.getContent(),siteUser);
        return "redirect:/question/detail/"+ q.getId();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String questionModify(QuestionForm questionForm, @PathVariable("id") Integer id, Principal principal) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = getSiteUser(principal);
        if(!question.getAuthor().getUsername().equals(siteUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        questionForm.setSubject(question.getSubject());
        questionForm.setContent(question.getContent());
        return "question_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String questionModify(@Valid QuestionForm questionForm, BindingResult bindingResult,
                                 Principal principal, @PathVariable("id") Integer id) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = getSiteUser(principal);

        if (!question.getAuthor().getUsername().equals(siteUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
        return String.format("redirect:/question/detail/%s", id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = getSiteUser(principal);

        if (!question.getAuthor().getUsername().equals(siteUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.questionService.delete(question);
        return "redirect:/";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String questionVote(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = getSiteUser(principal);
        this.questionService.vote(question, siteUser);
        return String.format("redirect:/question/detail/%s", id);
    }
}
