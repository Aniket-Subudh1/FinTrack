import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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
    const headers = this.createAuthorizationHeader();
    return this.http.get<UserDetailsResponse>(`${BASE_URL}/api/user/details`, { headers });
  }


  updateUserDetails(updateRequest: any): Observable<UserDetailsUpdateResponse> {
    const headers = this.createAuthorizationHeader();
    return this.http.put<UserDetailsUpdateResponse>(`${BASE_URL}/api/user/details`, updateRequest, { headers });
  }

  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    if (jwtToken) {
      return new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);
    } else {
      return new HttpHeaders();
    }
  }
}
