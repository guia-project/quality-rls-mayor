import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { NgStyle } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-createqr',
  standalone:true,
  imports: [ReactiveFormsModule, NgStyle],
  templateUrl: './createqr.html',
  styleUrl: './createqr.css',
})
export class Createqr {
  message = new FormControl('');
  tipo = new FormControl('');
  responseData: any = null;
  isError = false;
  constructor(private http: HttpClient, private cdr: ChangeDetectorRef){}
  send(){
    const body = {
      content: this.message.value,
      type: this.tipo.value
     }
  this.http.post('http://localhost:8080/qr', body)
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
