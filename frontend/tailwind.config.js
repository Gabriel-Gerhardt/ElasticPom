/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{html,js,svelte,ts}'],
	theme: {
		extend: {
			colors: {
				brand: {
					900: '#1A1A2E',
					800: '#16213E',
					700: '#0F3460'
				},
				accent: {
					500: '#E94560',
					400: '#FF6B81'
				},
				neutral: {
					100: '#F1F1F1',
					300: '#A8A8B3',
					600: '#3D3D5C'
				}
			}
		}
	},
	plugins: []
};
