package com.revhire.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        logger.error("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", 404);
        return "error";
    }

    @ExceptionHandler(BadRequestException.class)
    public String handleBadRequest(BadRequestException ex, Model model) {
        logger.error("Bad request: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Bad Request");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", 400);
        return "error";
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException ex, Model model) {
        logger.error("Forbidden access attempt: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", 403); // Change from 401 to 403
        return "error";
    }
    
//    handle 404 page not found errors
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public String handleNoHandlerFound(org.springframework.web.servlet.NoHandlerFoundException ex, Model model) {
        logger.error("404 Error - Page not found: {}", ex.getRequestURL());
        model.addAttribute("errorTitle", "Page Not Found");
        model.addAttribute("errorMessage", "The page you are looking for doesn't exist or has been moved.");
        model.addAttribute("errorCode", 404);
        return "error"; // Matches your return "error" pattern
    }
    
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public String handleStaticResourceNotFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, Model model) {
        logger.error("404 Resource Error: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Page Not Found");
        model.addAttribute("errorMessage", "RevHire couldn't find the resource you requested.");
        model.addAttribute("errorCode", 404);
        return "error";
    }
    
    
    @ExceptionHandler(FileStorageException.class)
    public String handleFileStorage(FileStorageException ex, RedirectAttributes redirectAttributes) {
        logger.error("File storage error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "File upload failed: " + ex.getMessage());
        return "redirect:/resume/upload";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
        logger.error("File size exceeded: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "File size exceeds the maximum limit of 2MB.");
        return "redirect:/resume/upload";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidation(MethodArgumentNotValidException ex, Model model) {
        logger.error("Validation error: {}", ex.getMessage());
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append(". ")
        );
        model.addAttribute("errorTitle", "Validation Error");
        model.addAttribute("errorMessage", errors.toString());
        model.addAttribute("errorCode", 400);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model, HttpServletRequest request) {
        logger.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        // Check if the exception is actually a 404 in disguise
        if (ex instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            model.addAttribute("errorTitle", "Page Not Found");
            model.addAttribute("errorMessage", "The page you requested does not exist.");
            model.addAttribute("errorCode", 404);
        } else {
            model.addAttribute("errorTitle", "Something Went Wrong");
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            model.addAttribute("errorCode", 500);
        }
        return "error";
    }
    
    

}
