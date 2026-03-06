package com.revhire.application.controller;

import com.revhire.application.dto.FavoriteResponse;
import com.revhire.application.service.FavoriteService;
import com.revhire.auth.entity.User;
import com.revhire.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Log4j2
@PreAuthorize("hasRole('SEEKER')")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;  // Add this

    @PostMapping("/{jobId}")
    public String saveJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "Referer", required = false) String referer,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            favoriteService.saveJob(userId, jobId);
            redirectAttributes.addFlashAttribute("successMessage", "Job saved to favorites!");
        } catch (Exception e) {
            log.error("Error saving job: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:" + (referer != null ? referer : "/jobs/search/results");
    }

    @PostMapping("/{jobId}/remove")
    public String removeJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "Referer", required = false) String referer,
            RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                return "redirect:/auth/login";
            }
            
            favoriteService.removeJob(userId, jobId);
            redirectAttributes.addFlashAttribute("successMessage", "Job removed from favorites!");
        } catch (Exception e) {
            log.error("Error removing job: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:" + (referer != null ? referer : "/favorites");
    }

    @GetMapping
    public String myFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Fetching favorites for user");
        
        try {
            Long userId = getCurrentUserId(currentUser);
            if (userId == null) {
                log.warn("User not authenticated, redirecting to login");
                return "redirect:/auth/login";
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("savedAt").descending());
            Page<FavoriteResponse> favorites = favoriteService.getMyFavorites(userId, pageable);
            
            model.addAttribute("favorites", favorites);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", favorites.getTotalPages());
            
            return "application/favorites";
            
        } catch (Exception e) {
            log.error("Error fetching favorites: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load favorites");
            return "redirect:/jobs/search";
        }
    }

    @GetMapping("/check/{jobId}")
    @ResponseBody
    public boolean checkFavorite(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User currentUser) {
        
        Long userId = getCurrentUserId(currentUser);
        if (userId == null) {
            return false;
        }
        return favoriteService.isJobFavorited(userId, jobId);
    }
    
    /**
     * Helper method to get current user ID from either AuthenticationPrincipal or SecurityContext
     */
    private Long getCurrentUserId(User currentUser) {
        if (currentUser != null) {
            log.info("Got user from AuthenticationPrincipal: {}", currentUser.getId());
            return currentUser.getId();
        }
        
        // Try SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            log.info("Got user from SecurityContext: {}", email);
            
            // Try to find user by email
            try {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    log.info("Found user with ID: {}", user.getId());
                    return user.getId();
                }
            } catch (Exception e) {
                log.error("Error finding user by email: {}", e.getMessage());
            }
        }
        
        log.warn("No authenticated user found");
        return null;
    }
}