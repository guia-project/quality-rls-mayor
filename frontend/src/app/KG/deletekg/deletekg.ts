import { Component } from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { NgStyle } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-deletekg',
  standalone:true,
  imports: [ReactiveFormsModule,NgStyle],
  templateUrl: './deletekg.html',
  styleUrl: './deletekg.css',
})
export class Deletekg {

  constructor(private http: HttpClient,private cdr: ChangeDetectorRef) {}
  kgid = new FormControl('');
  responseMessage= '';
  isError: boolean = false;

  delete(){
    const kgidValue = this.kgid.value;
    this.http.delete(`http://localhost:8080/kg/${kgidValue}`, {responseType: 'text'})
    .subscribe({
      next: (response: string) => {
      this.responseMessage = response;
      this.isError = false;
      this.cdr.detectChanges();
    },
    error: (err) => {
        if (typeof err.error === 'string') {
          this.responseMessage = err.error;
        } else {
          this.responseMessage = 'Error en la petición';
        }
        this.isError = true;
        this.cdr.detectChanges();
      }
    })
  }
}
