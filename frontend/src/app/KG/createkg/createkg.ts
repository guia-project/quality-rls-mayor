import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { NgStyle } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-createkg',
  standalone: true,
  imports: [ReactiveFormsModule, NgStyle],
  templateUrl: './createkg.html',
  styleUrl: './createkg.css',
})
export class Createkg {
  message = new FormControl('');
  responseData: any = null;
  isError = false;
  constructor(private http: HttpClient, private cdr: ChangeDetectorRef){}
  send(){
  this.http.post('http://localhost:8080/kg', this.message.value)
  .subscribe({
    next: (response: any) => {
      this.responseData = response;
      this.isError = false;
      this.cdr.detectChanges();
    },
    error: (err) => {
        if (typeof err.error === 'string') {
          this.responseData = err.error;
        } else {
          this.responseData = 'Error en la petición';
        }
        this.isError = true;
        this.cdr.detectChanges();
      }
    });
}
}
