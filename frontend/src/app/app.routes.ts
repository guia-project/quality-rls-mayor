import { Routes } from '@angular/router';
import { Createkg } from './KG/createkg/createkg';
import { Createqr } from './QR/createqr/createqr';
import { Home } from './home/home';
import { Getqr } from './QR/getqr/getqr';
import { Getkg } from './KG/getkg/getkg';
import { ResultGetQr } from './QR/getqr/result-get-qr/result-get-qr';
import { ResultGetKg } from './KG/getkg/result-get-kg/result-get-kg';
import { Validatekg } from './KG/validatekg/validatekg';
import { Deletekg } from './KG/deletekg/deletekg';
import { Deleteqr } from './QR/deleteqr/deleteqr';
import { KgHome } from './KG/kg-home/kg-home';
import { QrHome } from './QR/qr-home/qr-home';

export const routes: Routes = [
    {path:'',component:Home},
    {path:'ckg',component:Createkg},
    {path:'cqr',component:Createqr},
    {path:'gqr', component:Getqr},
    {path:'result-qr/:id', component:ResultGetQr},
    {path: 'result-qr', component: ResultGetQr},
    {path:'gkg', component:Getkg},
    {path:'result-kg/:id', component:ResultGetKg},
    {path: 'result-kg', component: ResultGetKg},
    {path:'validate', component:Validatekg},
    {path:'dkg', component:Deletekg},
    {path:'dqr', component:Deleteqr},
    {path:'kg',component:KgHome},
    {path:'qr',component:QrHome}


];
