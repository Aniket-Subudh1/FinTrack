import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE_URL = 'http://localhost:8080';  // Backend API URL

@Injectable({
  providedIn: 'root'
})
export class JwtService {

  constructor(private http: HttpClient) { }

  
  register(signRequest: any): Observable<any> {

    return this.http.post(`${BASE_URL}/signup`, signRequest, { responseType: 'text' });
  }

  login(loginRequest: any): Observable<any> {
    return this.http.post(`${BASE_URL}/login`, loginRequest);
  }

  private createAuthorizationHeader(): HttpHeaders {
    const jwtToken = localStorage.getItem('jwt');
    if (jwtToken) {
      console.log("JWT token found in local storage:", jwtToken);
      return new HttpHeaders().set('Authorization', `Bearer ${jwtToken}`);
    } else {
      console.log("JWT token not found in local storage");
      return new HttpHeaders();  
    }
  }
}
