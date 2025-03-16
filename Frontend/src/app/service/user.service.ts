import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserDetailsResponse {
  name: string;
  email: string;
  address: string;
  gender: string;
  age: number;
  profilePhoto?: string; 
}

export interface UserDetailsUpdateResponse {
  message: string;
}

const BASE_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  constructor(private http: HttpClient) {}
 
  getUserDetails(): Observable<UserDetailsResponse> {
    return this.http.get<UserDetailsResponse>(`${BASE_URL}/api/user/details`, { withCredentials: true });
  }

  updateUserDetails(updateRequest: any): Observable<UserDetailsUpdateResponse> {
    return this.http.put<UserDetailsUpdateResponse>(`${BASE_URL}/api/user/details`, updateRequest, { withCredentials: true });
  }
}