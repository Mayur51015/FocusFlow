import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'FocusFlow';
  private router = inject(Router);
  isPublicRoute = signal<boolean>(false);

  constructor() {
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe((event) => {
      const url = event.urlAfterRedirects.split('?')[0];
      const publicPaths = ['/', '/login', '/register', '/forgot-password', '/reset-password', '/access-denied'];
      this.isPublicRoute.set(publicPaths.includes(url));
    });
  }
}
