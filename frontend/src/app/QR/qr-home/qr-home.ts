import { Component } from '@angular/core';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-qr-home',
  standalone:true,
  imports: [RouterLink],
  templateUrl: './qr-home.html',
  styleUrl: './qr-home.css',
})
export class QrHome {}
