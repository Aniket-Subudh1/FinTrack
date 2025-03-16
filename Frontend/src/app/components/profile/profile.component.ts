import { Component, OnInit } from '@angular/core';
import { UserService } from '../../service/user.service';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, SidebarComponent, HttpClientModule, FormsModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  userDetailsForm: FormGroup;
  profilePhotoUrl: string | ArrayBuffer | null = null;
  isLoading: boolean = false;
  isSidebarOpen: boolean = true;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.userDetailsForm = this.fb.group({
      name: [{ value: '', disabled: true }, Validators.required],
      email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
      address: [''],
      gender: [''],
      age: ['', [Validators.min(1)]],
      profilePhoto: [null],
    });
  }

  ngOnInit(): void {
    this.loadUserDetails();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  loadUserDetails(): void {
    this.isLoading = true;
    console.log('Loading user details...');
    
    this.userService.getUserDetails()
      .pipe(
        catchError(error => {
          console.error('Error fetching user details:', error);
          this.showError('Failed to load user details: ' + (error.error?.message || 'Unknown error'));
          if (error.status === 401) {
            console.log('Unauthorized access - redirecting to login');
            this.router.navigate(['/login']);
          }
          return of(null);
        }),
        finalize(() => this.isLoading = false)
      )
      .subscribe(data => {
        if (data) {
          console.log('User details loaded successfully:', data);
          this.userDetailsForm.patchValue({
            name: data.name || '',
            email: data.email || '',
            address: data.address || '',
            gender: data.gender || '',
            age: data.age || '',
          });
          if (data.profilePhoto) {
            this.profilePhotoUrl = 'data:image/jpeg;base64,' + data.profilePhoto;
            console.log('Profile photo loaded');
          }
        }
      });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      console.log('Photo selected:', file.name);
      
      const fileNameElement = document.getElementById('fileName');
      if (fileNameElement) fileNameElement.textContent = file.name;
      
      const reader = new FileReader();
      reader.onload = (e) => {
        this.profilePhotoUrl = e.target?.result ?? null;
        if (typeof e.target?.result === 'string') {
          const base64String = e.target.result.split(',')[1];
          this.userDetailsForm.get('profilePhoto')?.setValue(base64String);
          console.log('Photo converted to base64');
        }
      };
      reader.readAsDataURL(file);
    }
  }

  saveChanges(): void {
    if (this.userDetailsForm.valid) {
      this.isLoading = true;
      console.log('Saving user details...');
      
      const updateRequest = {
        address: this.userDetailsForm.get('address')?.value,
        gender: this.userDetailsForm.get('gender')?.value,
        age: this.userDetailsForm.get('age')?.value,
        profilePhoto: this.userDetailsForm.get('profilePhoto')?.value,
      };

      console.log('Update request:', {
        address: updateRequest.address,
        gender: updateRequest.gender,
        age: updateRequest.age,
        profilePhoto: updateRequest.profilePhoto ? '[BASE64_DATA]' : null
      });

      this.userService.updateUserDetails(updateRequest)
        .pipe(
          catchError(error => {
            console.error('Update error:', error.status, error.message, error);
            this.showError('Failed to update user details: ' + (error.error?.message || 'Unknown error'));
            if (error.status === 401) {
              console.log('Unauthorized - redirecting to login');
              this.router.navigate(['/login']);
            }
            return of(null);
          }),
          finalize(() => this.isLoading = false)
        )
        .subscribe(response => {
          if (response) {
            console.log('User details updated successfully:', response);
            this.showSuccess('User details updated successfully!');
            this.profilePhotoUrl = response.profilePhoto ? 'data:image/jpeg;base64,' + response.profilePhoto : this.profilePhotoUrl;
          }
        });
    } else {
      this.showError('Please fill in all required fields correctly.');
    }
  }

  private showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    setTimeout(() => this.successMessage = '', 3000);
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    setTimeout(() => this.errorMessage = '', 3000);
  }
}