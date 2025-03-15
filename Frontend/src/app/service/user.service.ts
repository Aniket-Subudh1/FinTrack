import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';

export interface UserDetailsResponse {
  id: number;
  name: string;
  email: string;
  address: string;
  gender: string;
  age: number;
  profilePhoto?: string; 
}

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}
 
  getUserDetails(): Observable<UserDetailsResponse> {
    return this.http.get<UserDetailsResponse>(`${this.apiUrl}/users/details`);
  }

  updateUserDetails(updateRequest: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/users/details`, updateRequest);
  }

  // Helper method to create authorization headers
  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    if (jwtToken) {
      return new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);
    } else {
      return new HttpHeaders();
    }
  }
}