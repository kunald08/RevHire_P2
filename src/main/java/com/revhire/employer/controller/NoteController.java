package com.revhire.employer.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.revhire.employer.entity.ApplicantNote;
import com.revhire.employer.repository.ApplicantNoteRepository;
import com.revhire.employer.service.ApplicantService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/employer/notes") // Base path
@RequiredArgsConstructor
public class NoteController {

    private final ApplicantService applicantService;

    // This now maps to POST /employer/notes
//    @PostMapping("")
//    @ResponseBody // Tells Spring to return data, not a view
//	public ResponseEntity<String> addNote(@RequestParam Long appId, 
//	                                      @RequestParam String note, 
//	                                      Authentication auth) {
//	    applicantService.addNote(appId, note, auth.getName());
//	    return ResponseEntity.ok("Success");
//	}
//	
//	@PostMapping("/{id}/update")
//	@ResponseBody
//	public ResponseEntity<String> updateNote(@PathVariable Long id, 
//	                                         @RequestParam String noteContent) {
//	    applicantService.updateNote(id, noteContent);
//	    return ResponseEntity.ok("Updated");
//	}
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<String> saveNote(@RequestParam Long appId, 
                                           @RequestParam String note, 
                                           Authentication auth) {
        applicantService.saveOrUpdateNote(appId, note, auth.getName());
        System.out.println("DEBUG: Save request received for AppId: " + appId + " with Note: " + note);
        return ResponseEntity.ok("Saved successfully");
    }
}