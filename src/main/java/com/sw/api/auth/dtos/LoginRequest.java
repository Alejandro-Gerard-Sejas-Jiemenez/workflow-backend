/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.sw.api.auth.dtos;

public record LoginRequest(String email, String password) {
	public String getEmail() {
		return email;
	}
}

