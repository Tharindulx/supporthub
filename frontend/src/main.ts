import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { AppComponent } from './app/app.component';
bootstrapApplication(AppComponent, {providers:[provideHttpClient(withXsrfConfiguration({cookieName:'XSRF-TOKEN',headerName:'X-XSRF-TOKEN'}))]}).catch(console.error);
