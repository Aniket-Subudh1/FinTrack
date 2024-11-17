import { Component, OnInit } from '@angular/core';
import { UserService } from '../../service/user.service';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { SidebarComponent } from '../../pages/dashboard/sidebar/sidebar.component';
import { Router } from '@angular/router';

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
      address: ['', Validators.required],
      gender: ['', Validators.required],
      age: ['', [Validators.required, Validators.min(1)]],
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
    this.userService.getUserDetails().subscribe(
      (data: any) => {
        this.userDetailsForm.patchValue({
          name: data.name,
          email: data.email,
          address: data.address,
          gender: data.gender,
          age: data.age,
        });
        if (data.profilePhoto) {
          this.profilePhotoUrl = 'data:image/jpeg;base64,' + data.profilePhoto;
        }
      },
      (error) => {
        this.errorMessage = 'Failed to load user details. Please try again.';
        console.error('Error fetching user details:', error);
        setTimeout(() => (this.errorMessage = ''), 3000);
      }
    );
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();

      reader.onload = (e) => {
        this.profilePhotoUrl = e.target?.result ?? null;
        this.userDetailsForm.get('profilePhoto')?.setValue(
          (e.target?.result as string).split(',')[1]
        );
      };

      reader.readAsDataURL(file);
    }
  }

  saveChanges(): void {
    if (this.userDetailsForm.valid) {
      // Extract only the editable fields
      const updateRequest = {
        address: this.userDetailsForm.get('address')?.value,
        gender: this.userDetailsForm.get('gender')?.value,
        age: this.userDetailsForm.get('age')?.value,
        profilePhoto: this.userDetailsForm.get('profilePhoto')?.value,
      };

      this.userService.updateUserDetails(updateRequest).subscribe(
        (response) => {
          this.successMessage = 'User details updated successfully!';
          console.log('User details updated successfully:', response);
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        (error) => {
          this.errorMessage = 'Failed to update user details. Please try again.';
          console.error('Error updating user details:', error);
          setTimeout(() => (this.errorMessage = ''), 3000);
        }
      );
    } else {
      this.errorMessage = 'Please fill in all required fields correctly.';
      setTimeout(() => (this.errorMessage = ''), 3000);
    }
  }
}
