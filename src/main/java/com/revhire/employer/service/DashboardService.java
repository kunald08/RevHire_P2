package com.revhire.employer.service;

import com.revhire.employer.dto.DashboardStats;

public interface DashboardService {

    DashboardStats getEmployerDashboardStats(String email);

}