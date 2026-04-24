import { Component } from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { NgStyle } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-validatekg',
  standalone:true,
  imports: [ReactiveFormsModule, NgStyle],
  templateUrl: './validatekg.html',
  styleUrl: './validatekg.css',
})
export class Validatekg {
  
  constructor(private http: HttpClient,private cdr: ChangeDetectorRef) {}

  kgid =  new FormControl('');
  qrid = new FormControl('');
  responseMessage= '';
  isError: boolean = false;

  send(){
    const kgidValue = this.kgid.value;
    const qridValue = this.qrid.value;

    this.http.get(`http://localhost:8080/qr/validate/${kgidValue}/${qridValue}`, {responseType: 'text'})
    .subscribe({
      next: (response: string) => {
      this.responseMessage = "Knowledge Graph Validado";
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
  



